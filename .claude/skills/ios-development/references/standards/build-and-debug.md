# 编译、运行与调试 — iOS

> 本文档定义 iOS 端的构建、运行与调试规范。

---

## 1. 工程配置

### 1.1 XcodeGen

- 使用 **XcodeGen** 通过 `project.yml` 生成 `.xcodeproj`，避免 Xcode 工程文件冲突。
- `project.yml` 放在 `ios/project.yml` 中，关键配置示例：

```yaml
name: ShortDrama
options:
  bundleIdPrefix: com.djs66256
  deploymentTarget:
    iOS: "17.0"
  xcodeVersion: "15.0"

settings:
  SWIFT_VERSION: "5.9"

targets:
  ShortDrama:
    type: application
    platform: iOS
    sources:
      - path: ShortDrama
    settings:
      base:
        INFOPLIST_FILE: ShortDrama/Info.plist
        PRODUCT_BUNDLE_IDENTIFIER: com.djs66256.short_drama
        DEVELOPMENT_TEAM: "${DEV_TEAM}"
    dependencies:
      - package: Alamofire
      - package: Kingfisher
      - package: KeychainAccess
    preBuildScripts:
      - name: SwiftLint
        script: |
          if which swiftlint > /dev/null; then
            swiftlint lint --config ../.swiftlint.yml --strict
          else
            echo "warning: SwiftLint not installed"
          fi

packages:
  Alamofire:
    url: https://github.com/Alamofire/Alamofire
    from: "5.9.0"
  Kingfisher:
    url: https://github.com/onevcat/Kingfisher
    from: "7.12.0"
  KeychainAccess:
    url: https://github.com/kishikawakatsumi/KeychainAccess
    from: "4.2.2"
```

- 每次修改工程配置（新增文件、依赖）后执行 `cd ios && xcodegen generate` 重新生成。
- `.xcodeproj` 不提交到 Git（通过 `.gitignore` 排除），仅提交 `project.yml`。

### 1.2 Build Settings

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `IPHONEOS_DEPLOYMENT_TARGET` | `17.0` | 最低部署目标 |
| `SWIFT_VERSION` | `5.9` | Swift 语言版本 |
| `SWIFT_COMPILATION_MODE` | Debug: `incremental` / Release: `wholemodule` | 编译模式 |
| `SWIFT_OPTIMIZATION_LEVEL` | Debug: `-Onone` / Release: `-Osize` | 优化等级 |
| `ENABLE_MODULE_VERIFIER` | `YES` | 启用模块验证 |
| `CLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER` | `YES` | 引用检查 |
| `SWIFT_ACTIVE_COMPILATION_CONDITIONS` | Debug: `DEBUG` | 编译条件 |
| `DEBUG_INFORMATION_FORMAT` | Debug: `dwarf` / Release: `dwarf-with-dsym` | 符号格式 |

### 1.3 Signing

- Debug 使用 **自动签名**（Automatic Signing），Xcode 自动管理 Provisioning Profile。
- Release / Archive 使用 **手动签名**，由 CI 通过 Fastlane `match` 管理证书和 Profile。
- 签名配置在 `project.yml` 中通过环境变量 `DEV_TEAM` 传递 Team ID。
- 禁止将 `.p12` 证书文件或 `Provisioning Profile` 提交到仓库。
- 如需在 CI 环境签名，通过 Fastfile 调用 `match(type: "appstore", readonly: true)`。

### 1.4 依赖管理

- 使用 **Swift Package Manager (SPM)** 管理所有第三方依赖。
- 在 `project.yml` 中声明 package 引用，XcodeGen 生成时自动解析。
- 解析后的 `Package.resolved` 需提交到 Git（锁定版本）。
- 不从本地拷贝源码或手动拖拽 framework——统一走 SPM。
- 新增依赖前需经团队评审（open-source-libs.md 中 ⚠️ 标记的库需用户同意）。

---

## 2. 编译命令

### 2.1 xcodebuild

```bash
# Debug 构建（模拟器）
xcodebuild build \
  -project ios/ShortDrama.xcodeproj \
  -scheme ShortDrama \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro,OS=17.4' \
  -configuration Debug \
  -derivedDataPath .build

# Archive（App Store 包）
xcodebuild archive \
  -project ios/ShortDrama.xcodeproj \
  -scheme ShortDrama \
  -destination 'generic/platform=iOS' \
  -configuration Release \
  -archivePath .build/ShortDrama.xcarchive

# 单元测试
xcodebuild test \
  -project ios/ShortDrama.xcodeproj \
  -scheme ShortDrama \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro,OS=17.4' \
  -resultBundlePath .build/test_result.xcresult

# 查看可用模拟器
xcodebuild -showdestinations \
  -project ios/ShortDrama.xcodeproj \
  -scheme ShortDrama
```

### 2.2 Fastlane

```bash
# 运行测试
cd ios && bundle exec fastlane test

# 构建 App Store 包
cd ios && bundle exec fastlane build_release

# 上传到 TestFlight
cd ios && bundle exec fastlane beta
```

Fastlane Fastfile 关键 lane：

```ruby
# fastlane/Fastfile
default_platform(:ios)
platform :ios do
  lane :test do
    run_tests(
      scheme: "ShortDrama",
      devices: ["iPhone 15 Pro"],
      derived_data_path: ".build"
    )
  end

  lane :build_release do
    build_app(
      scheme: "ShortDrama",
      export_method: "app-store",
      archive_path: ".build/ShortDrama.xcarchive",
      output_directory: ".build/output"
    )
  end

  lane :beta do
    build_release
    upload_to_testflight
  end
end
```

### 2.3 代码检查

```bash
# SwiftLint 检查（CI 用 --strict 使 warning 变 error）
swiftlint lint --config ios/.swiftlint.yml --strict

# SwiftLint 自动修复
swiftlint --fix --config ios/.swiftlint.yml

# Swift 格式化（使用 swift-format）
swift-format lint -r ios/ShortDrama/
swift-format format -i -r ios/ShortDrama/
```

---

## 3. 运行与调试

### 3.1 模拟器

```bash
# 列出所有可用设备
xcrun simctl list devices available

# 列出已启动的模拟器
xcrun simctl list devices | grep Booted

# 创建新模拟器
xcrun simctl create "ShortDrama Test" "iPhone 15 Pro" "com.apple.CoreSimulator.SimRuntime.iOS-17-4"

# 启动模拟器
xcrun simctl boot "ShortDrama Test"
open -a Simulator

# 关机
xcrun simctl shutdown "iPhone 15 Pro"

# 删除模拟器
xcrun simctl delete "ShortDrama Test"

# 擦除所有数据
xcrun simctl erase "iPhone 15 Pro"

# 查看日志路径
xcrun simctl getenv booted SIMULATOR_LOG_ROOT
```

### 3.2 LLDB 调试

```bash
# 常用 LLDB 命令（在 Xcode 控制台或 lldb 终端中）：
(lldb) po viewModel              # 打印对象
(lldb) po viewModel.dramas.count # 打印属性
(lldb) p viewModel.isLoading     # 打印值
(lldb) expr -l Swift -- let x = ... # 执行 Swift 表达式
(lldb) frame variable            # 查看当前栈帧变量
(lldb) frame variable -O self    # 用对象描述打印 self
(lldb) bt                        # 查看调用栈
(lldb) thread return             # 跳过当前方法直接返回
(lldb) breakpoint set -n fetchDramas # 在函数名设断点
(lldb) breakpoint set -f HomeViewModel.swift -l 42 # 在文件第 42 行设断点
(lldb) breakpoint set -F '-[UIView setFrame:]' # 在 ObjC 方法设断点
(lldb) watchpoint set variable self.items.count   # 监视变量变化
(lldb) image lookup -rn "Kingfisher" # 查找符号
```

### 3.3 Xcode Debug

- **View Debugging**：运行 App 后点击 Debug Bar → "Debug View Hierarchy"，检查 View 层级、约束（或 SwiftUI 实际渲染的 frame）。
- **Memory Graph**：Debug Navigator → Memory → 右上角相机图标。用于检测循环引用、内存泄漏。
- **Network Debugging**：
  - 简单请求：使用 Xcode 自带的 Network tab（Debug Navigator → Network）。
  - 更详细：集成 Pulse 库（open-source-libs.md 🔶）在调试期间拦截所有网络请求。
  - 外部代理：使用 Proxyman / Charles 抓取 HTTPS 流量（需安装并信任证书）。
- **Debug Gauges**：监控 CPU、内存、能耗、网络、磁盘 I/O 实时指标。

### 3.4 无线调试

- 首次连接需用 USB 线连接真机。
- Xcode → Window → Devices and Simulators → 勾选 "Connect via network"。
- 之后同 WiFi 下可无线调试。
- 如连接不稳定，关闭 WiFi 的 "Low Data Mode"，或在路由器设置中调整。
- WiFi 调试下 LLDB 和日志正常可用，但安装 .app 速度较 USB 慢。

---

## 4. 性能分析

### 4.1 Instruments

启动 Instruments：`Xcode → Product → Profile (Cmd+I)`，选择模板：

| 模板 | 用途 | 关注指标 |
|------|------|----------|
| **Time Profiler** | 分析 CPU 时间分布 | 主线程热点函数、不必要的重复计算 |
| **Allocations** | 内存分配与泄漏 | Persistent Bytes、# Persistent、Leaks |
| **Leaks** | 检测循环引用 | 红色柱子 = 泄漏对象 |
| **Energy Log** | 能耗分析 | CPU 唤醒次数、GPU 占用、网络请求频率 |
| **System Trace** | 全面系统行为追踪 | 线程切换、上下文开销、I/O 等待 |
| **SwiftUI** | SwiftUI body 重绘 | body 调用频率、无效重绘 |

- 列表滚动检查重点：在 Time Profiler 中查看是否有重复的 `body` 调用或解码开销。
- 视频播放检查重点：Energy Log 中的 Decode 和 Render 分布。

### 4.2 Xcode Organizer

- Xcode → Window → Organizer → 选择 ShortDrama。
- **Crashes**：查看 TestFlight / App Store 用户的崩溃报告，按版本和设备筛选。
- **Energy**：查看用户设备上的能耗报告（包括 CPU、网络、定位、后台唤醒）。
- **Metrics → Launch Time**：查看用户实际启动耗时，关注 p95 和平均值。
- **Battery Usage**：查看后台耗电异常（如后台持续播放或网络请求）。

### 4.3 MetricKit

- 注册 MetricKit 订阅：

```swift
import MetricKit

final class MetricsManager: NSObject, MXMetricManagerSubscriber {
    override init() {
        super.init()
        MXMetricManager.shared.add(self)
    }

    func didReceive(_ payloads: [MXDiagnosticPayload]) {
        for payload in payloads {
            // 处理 crashDiagnostics、hangDiagnostics、cpuExceptionDiagnostics
            if let crashes = payload.crashDiagnostics {
                for crash in crashes {
                    Logger.metrics.error("Crash: \(crash.terminationReason ?? "unknown")")
                }
            }
        }
    }
}
```

- 关注指标：启动耗时（`MXAppLaunchMetric`）、CPU 使用（`MXCPUMetric`）、内存峰值（`MXMemoryMetric`）、网络数据传输量（`MXNetworkTransferMetric`）。
- 接收到的 Metrics 数据可通过自定义后端上报，用于线上性能大盘。
