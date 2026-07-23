# 安全 — iOS

> 本文档定义 iOS 端的安全规范。

---

## 1. 数据安全

### 1.1 Keychain

- 敏感数据（Auth Token、Refresh Token、User ID）必须存储在 Keychain 中，禁止使用 UserDefaults。
- 使用 **KeychainAccess** 封装 Keychain 操作：

```swift
import KeychainAccess

final class KeychainManager {
    static let shared = KeychainManager()

    private let keychain = Keychain(service: "com.djs66256.short_drama")
        .accessibility(.whenUnlockedThisDeviceOnly)
        .synchronizable(false)

    var authToken: String? {
        get { try? keychain.get("auth_token") }
        set { try? keychain.set(newValue ?? "", key: "auth_token") }
    }

    var refreshToken: String? {
        get { try? keychain.get("refresh_token") }
        set { try? keychain.set(newValue ?? "", key: "refresh_token") }
    }

    var userId: String? {
        get { try? keychain.get("user_id") }
        set { try? keychain.set(newValue ?? "", key: "user_id") }
    }

    func clearAll() {
        try? keychain.removeAll()
    }
}
```

- `.accessibility(.whenUnlockedThisDeviceOnly)`：仅设备解锁时可访问，不迁移到其他设备。
- `.synchronizable(false)`：不同步到 iCloud。
- 用户退出登录时调用 `clearAll()`。
- 不得在 Keychain 中持久化大型数据（如完整 JSON 对象、图片 Base64）。

### 1.2 加密方案

- 使用 Apple 原生的 **CryptoKit** 处理加密需求：

```swift
import CryptoKit

// SHA-256 哈希（用于 API 签名、校验和）
func sha256(_ input: String) -> String {
    let digest = SHA256.hash(data: Data(input.utf8))
    return digest.compactMap { String(format: "%02x", $0) }.joined()
}

// HMAC 签名
func hmacSHA256(key: SymmetricKey, message: String) -> Data {
    let signature = HMAC<SHA256>.authenticationCode(for: Data(message.utf8), using: key)
    return Data(signature)
}

// AES-GCM 加密（传输敏感数据）
func encryptAES(_ plaintext: Data, key: SymmetricKey) throws -> Data {
    let sealedBox = try AES.GCM.seal(plaintext, using: key)
    return sealedBox.combined!
}

func decryptAES(_ ciphertext: Data, key: SymmetricKey) throws -> Data {
    let sealedBox = try AES.GCM.SealedBox(combined: ciphertext)
    return try AES.GCM.open(sealedBox, using: key)
}
```

- 不要自行实现加密算法——CryptoKit 覆盖了所有通用场景。
- 不要使用已废弃的 `CommonCrypto`（C API）——优先 CryptoKit。
- 对称加密的 Key 存储在 Keychain 中，不硬编码在代码中。

### 1.3 数据保护

- 文件级别的 Data Protection 设置：
  - 用户数据文件（Core Data .sqlite）：`FileProtection.completeUntilFirstUserAuthentication`（默认）。
  - 缓存、日志等非敏感文件：`FileProtection.none`。
- 在创建文件时显式设置保护等级：

```swift
let fileURL = documentsDirectory.appendingPathComponent("user_data.json")
try data.write(to: fileURL, options: .completeFileProtection)
```

- 所有本地数据库文件（.sqlite、.sqlite-wal、.sqlite-shm）使用默认的 complete protection。
- App 退到后台时清除剪贴板中的敏感内容（如密码、验证码）。
- 不在 NSLog / os_log 中输出完整 Token、用户手机号等敏感信息（使用 `%{private}@` 格式符）。

---

## 2. 网络安全

### 2.1 ATS (App Transport Security)

- 生产环境中 **不允许** 禁用 ATS。所有 HTTPS 请求使用 TLS 1.2+。
- `Info.plist` 中 `NSAppTransportSecurity` 保持默认（即不配置 `NSAllowsArbitraryLoads`）：
  ```xml
  <key>NSAppTransportSecurity</key>
  <dict>
      <key>NSAllowsArbitraryLoads</key>
      <false/>
  </dict>
  ```
- 若开发阶段需要连接内网 HTTP 服务，使用 `NSExceptionDomains` 精确指定域名：
  ```xml
  <key>NSAppTransportSecurity</key>
  <dict>
      <key>NSExceptionDomains</key>
      <dict>
          <key>dev.local</key>
          <dict>
              <key>NSExceptionAllowsInsecureHTTPLoads</key>
              <true/>
          </dict>
      </dict>
  </dict>
  ```
- 提交 Release 前检查 `NSExceptionDomains` 是否已清理。

### 2.2 SSL Pinning

- 使用 Alamofire 的 `ServerTrustManager` 实现 SSL Pinning：

```swift
import Alamofire

final class APISession {
    let session: Session

    init() {
        let evaluators: [String: ServerTrustEvaluating] = [
            "api.shortdrama.example.com": PublicKeysTrustEvaluator(validateHost: true),
            // 可以使用 PinnedCertificatesTrustEvaluator 绑定完整证书
        ]
        let trustManager = ServerTrustManager(evaluators: evaluators)

        session = Session(serverTrustManager: trustManager)
    }
}
```

- **推荐 `PublicKeysTrustEvaluator`**：绑定公钥，证书更新时不需要重新发版。
- 证书/公钥文件放在 `ios/ShortDrama/Resources/Certificates/` 目录。
- SSL Pinning 只在 Release 构建中启用；Debug 构建跳过（方便使用 Charles / Proxyman 调试）。

### 2.3 证书校验

- 自定义 `URLSessionDelegate` 实现额外的证书验证逻辑（如果需要更细粒度的控制）：

```swift
final class CertificateValidator: NSObject, URLSessionDelegate {
    func urlSession(_ session: URLSession,
                    didReceive challenge: URLAuthenticationChallenge,
                    completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        guard challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust,
              let serverTrust = challenge.protectionSpace.serverTrust else {
            completionHandler(.cancelAuthenticationChallenge, nil)
            return
        }
        // 验证证书链
        let isValid = SecTrustEvaluateWithError(serverTrust, nil)
        completionHandler(isValid ? .useCredential : .cancelAuthenticationChallenge,
                          isValid ? URLCredential(trust: serverTrust) : nil)
    }
}
```

- 服务器证书过期时，后端团队需提前 7 天通知客户端团队更新 Pinning。

---

## 3. 代码安全

### 3.1 代码混淆

- Swift 编译后符号已被 mangling，但仍可从二进制文件还原出大致结构。
- 增强混淆措施：
  - 关键字符串（API endpoint path、加密常量）不要以明文字面量写代码中——从加密配置文件或 Keychain 中读取。
  - 敏感逻辑（加密、验签）的类名和方法名避免使用直白的名称如 `Decryptor` / `verifySignature`，可使用组合式的业务名如 `PackageTransformer` / `checkIntegrity`。
- 不推荐引入第三方混淆工具（如 iXGuard）增加构建复杂度——当前阶段以最低必要成本提高逆向门槛。

### 3.2 反调试

- 反调试机制（按需）：
  - 检测调试器挂载：
    ```swift
    import Darwin
    func isDebuggerAttached() -> Bool {
        var info = kinfo_proc()
        var size = MemoryLayout<kinfo_proc>.size
        var mib: [Int32] = [CTL_KERN, KERN_PROC, KERN_PROC_PID, getpid()]
        sysctl(&mib, 4, &info, &size, nil, 0)
        return (info.kp_proc.p_flag & P_TRACED) != 0
    }
    ```
  - 仅在 Release 构建中启用，且仅作为辅助防护层——不依赖此机制保证安全性。
  - 反调试代码统一放在 `Core/Security/DebugDetector.swift` 中。

### 3.3 敏感字符串

- **禁止**以下行为：
  - API Key、Client Secret 硬编码在 Swift 源文件中。
  - 服务端地址、CDN 域名在代码中以明文字符串定义。
  - 测试用的 Token 或用户凭证提交到 Git。
- 敏感配置管理：
  - 通过 `ios/Configuration/` 目录按环境（Debug / Release）放置 `.xcconfig` 文件。
  - 在 xcconfig 中将 API Key 等定义为编译变量，在 `Info.plist` 中通过 `${VAR_NAME}` 引用。
  - xcconfig 中带 Secret 的配置文件不提交 Git（加入 `.gitignore`），仅在 CI 环境变量中注入。
- 代码审查中扫描 `key`、`secret`、`token`、`password` 关键词，确保无硬编码。

---

## 4. 运行时安全

### 4.1 越狱检测

- 按需实施，非必选项。若产品要求安全策略，可检测以下特征：
  ```swift
  final class JailbreakDetector {
      static func isJailbroken() -> Bool {
          // 检查越狱常用路径
          let paths = [
              "/Applications/Cydia.app",
              "/Library/MobileSubstrate/MobileSubstrate.dylib",
              "/bin/bash",
              "/usr/sbin/sshd",
              "/etc/apt",
              "/private/var/lib/apt"
          ]
          for path in paths {
              if FileManager.default.fileExists(atPath: path) { return true }
          }
          // 检查能否写入系统目录（越狱环境可写）
          let testPath = "/private/jailbreak_test"
          do {
              try "test".write(toFile: testPath, atomically: true, encoding: .utf8)
              try FileManager.default.removeItem(atPath: testPath)
              return true
          } catch {
              return false
          }
      }
  }
  ```
- 检测结果仅作为风控信号之一上报，不在客户端直接阻断功能。
- 越狱检测代码统一在 `Core/Security/JailbreakDetector.swift`，方便维护和去除。

### 4.2 截屏防护

- 监听截屏事件：
  ```swift
  NotificationCenter.default.addObserver(
      forName: UIApplication.userDidTakeScreenshotNotification,
      object: nil,
      queue: .main
  ) { _ in
      // 截屏后处理：隐藏敏感信息、记录日志
      Logger.security.notice("用户截屏")
  }
  ```
- 防止敏感页面被录屏：
  - 关键页面（如支付确认、Token 展示）使用 `UITextField.secureTextEntry` 阻止 iOS 录屏捕获。
  - 极端场景下使用 `UIScreen.capturedDidChangeNotification` 监听录屏状态：
    ```swift
    NotificationCenter.default.addObserver(
        forName: UIScreen.capturedDidChangeNotification,
        object: nil,
        queue: .main
    ) { _ in
        let isCaptured = UIScreen.main.isCaptured
        if isCaptured {
            // 模糊敏感内容
            blurSensitiveContent()
        } else {
            removeBlur()
        }
    }
    ```
- App 进入后台时，在 `scenePhase` 变化到 `.background` 时覆盖模糊层，防止多任务切换时显示敏感内容预览。
