# 安全 — Android

> 本文档定义 Android 端的安全规范。

---

## 1. 数据安全

### 1.1 敏感数据存储

**加密 KV 存储**：Token、用户手机号等敏感信息使用 EncryptedSharedPreferences。

```kotlin
// 初始化（Application.onCreate 中）
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
)

// 封装为 DataStore 风格的接口
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getToken(): String? = prefs.getString("auth_token", null)

    fun setToken(token: String?) {
        prefs.edit().putString("auth_token", token).apply()
    }

    fun clear() = prefs.edit().clear().apply()
}
```

**敏感数据分级与存储方案**：

| 数据级别 | 示例 | 存储方案 |
|---------|------|---------|
| 高敏感 | 登录 Token、Refresh Token、支付密钥 | EncryptedSharedPreferences（AES256） |
| 中敏感 | 用户手机号、观看历史、收藏列表 | Room（明文，但数据库文件在应用沙箱内） |
| 低敏感 | 浏览偏好、主题设置 | Preferences DataStore（明文） |

**禁止行为**：
- 禁止将 Token 存储在 `SharedPreferences`（无加密）中。
- 禁止将 Token 拼在 URL 参数中（应放在 Header `Authorization` 中）。
- 禁止在 Log 中输出 Token（明文）。
- 禁止将用户敏感数据写入外部存储（`/sdcard/`）。

### 1.2 加密方案

**场景选择**：

| 算法 | 场景 | 用途 |
|------|------|------|
| AES256-GCM | 本地数据加密（EncryptedSharedPreferences、文件加密） | 对称加密，速度快 |
| RSA-2048 | 传输密钥加密、客户端证书 | 非对称加密 |
| SHA-256 | 密码哈希、文件完整性校验 | 单向不可逆 |
| HMAC-SHA256 | API 签名、请求防篡改 | 带密钥的消息认证码 |

**文件加密**：对于需要加密的缓存文件（如离线视频），使用 Jetpack Security 的 `EncryptedFile`：

```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedFile = EncryptedFile.Builder(
    context,
    File(filesDir, "offline_video.db"),
    masterKey,
    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
).build()

// 写入
encryptedFile.openFileOutput().use { outputStream ->
    outputStream.write(data)
}

// 读取
encryptedFile.openFileInput().use { inputStream ->
    val decryptedData = inputStream.readBytes()
}
```

**密钥管理原则**：
- MasterKey 由 Android Keystore 系统保护（硬件级别），应用无法导出。
- 不自行实现加密算法，必须使用经过审计的标准库（Jetpack Security / javax.crypto）。
- 不硬编码密钥、盐值、IV，通过 Keystore 动态生成。

### 1.3 剪贴板

**安全策略**：
- 敏感内容（如登录密码、邀请码）禁止写入系统剪贴板。
- 如需复制功能（如分享链接），应在应用切换到后台时自动清空：
  ```kotlin
  // 在 Activity.onPause 或 ProcessLifecycleOwner 中
  if (lifecycle.currentState == Lifecycle.State.CREATED) {
      val clipboard = context.getSystemService(ClipboardManager::class.java)
      clipboard.clearPrimaryClip()
  }
  ```
- 从剪贴板读取内容前检查来源（`clipData.description.label`），若来自不可信应用则警告用户。

---

## 2. 网络安全

### 2.1 SSL Pinning

使用 OkHttp 的 `CertificatePinner` 实现证书绑定，防御中间人攻击。

```kotlin
// NetworkModule.kt
@Provides
@Singleton
fun provideOkHttpClient(...): OkHttpClient = OkHttpClient.Builder()
    .certificatePinner(
        CertificatePinner.Builder()
            .add(
                "api.shortdrama.example.com",
                "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="  // 后端证书公钥指纹
            )
            .add(
                "cdn.shortdrama.example.com",
                "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
            )
            .build()
    )
    .build()
```

**证书指纹获取方法**：

```bash
# 从 HTTPS 域名获取证书指纹
openssl s_client -servername api.shortdrama.example.com \
    -connect api.shortdrama.example.com:443 2>/dev/null \
    | openssl x509 -pubkey -noout \
    | openssl pkey -pubin -outform der \
    | openssl dgst -sha256 -binary \
    | openssl enc -base64
```

**注意事项**：
- 在 `BuildConfig.DEBUG` 时禁用 SSL Pinning（方便抓包调试）：
  ```kotlin
  val builder = OkHttpClient.Builder()
  if (!BuildConfig.DEBUG) {
      builder.certificatePinner(/* ... */)
  }
  ```
- 后端证书更新时需同步更新应用中的指纹，建议后端保留 1-2 周的重叠期（同时信任新旧证书）。
- 客户端保留备用证书指纹（Backup pin），防止服务端换证导致应用全部不可用。

### 2.2 证书校验

通过 `network_security_config.xml` 配置网络安全策略。

**res/xml/network_security_config.xml**：

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- 默认：拒绝明文流量 -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <!-- 仅信任系统 CA（不信任用户安装的证书） -->
            <certificates src="system" />
        </trust-anchors>
    </base-config>

    <!-- Debug 模式：允许特定域名明文（本地开发/测试服务器） -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.shortdrama.example.com</domain>
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </domain-config>

    <!-- Debug 覆盖：允许 Charles/Fiddler 抓包 -->
    <debug-overrides>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </debug-overrides>

    <!-- 钉住证书（Android 7+ 原生支持） -->
    <domain-config>
        <domain includeSubdomains="true">api.shortdrama.example.com</domain>
        <pin-set expiration="2026-12-31">
            <pin digest="SHA-256">base64_encoded_pin_here</pin>
            <pin digest="SHA-256">backup_pin_here</pin>
        </pin-set>
    </domain-config>
</network-security-config>
```

**AndroidManifest.xml 中引用**：

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
</application>
```

### 2.3 明文传输

**原则：禁止一切 HTTP 明文传输。**

- 生产环境所有 API 请求、图片、视频流均使用 HTTPS。
- `network_security_config.xml` 中 `cleartextTrafficPermitted="false"` 为全局默认。
- **例外**：仅 Debug 构建允许特定 IP（如 `10.0.2.2` 模拟器本地映射）的明文，且必须通过 `res/xml/network_security_config_debug.xml` 覆盖。

```xml
<!-- res/xml/network_security_config_debug.xml（仅在 debug 引入） -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
        <domain includeSubdomains="false">localhost</domain>
    </domain-config>
</network-security-config>
```

**禁止事项**：
- 禁止设置 `android:usesCleartextTraffic="true"`（全局允许明文）。
- 禁止为生产 Server 配置 HTTP 降级（HTTPS → HTTP 重定向应视为攻击）。
- WebView 中禁止加载 `http://` 页面（设置 `android:usesCleartextTraffic="false"` 或 WebView 的 `MixedContentMode`）。

---

## 3. 代码安全

### 3.1 混淆

使用 R8（Android Gradle Plugin 内置）进行代码混淆和压缩。

**ProGuard 规则文件位置**：

```
app/
├── proguard-rules.pro                       # 自有代码规则
├── proguard-rules-retrofit.pro             # Retrofit 保留规则
├── proguard-rules-room.pro                  # Room 保留规则
├── proguard-rules-hilt.pro                  # Hilt 保留规则
└── proguard-rules-coil.pro                  # Coil 保留规则
```

**关键 ProGuard 规则**：

```proguard
# proguard-rules.pro

# 保留 @Serializable 数据类（Kotlinx Serialization 依赖反射 metadata）
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class com.djs66256.short_drama.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 保留 Retrofit 接口（动态代理）
-keep,allowobfuscation interface com.djs66256.short_drama.data.remote.**ApiService

# 保留 Room Entity（Room 编译期生成代码访问）
-keep class com.djs66256.short_drama.data.local.entity.** { *; }

# Hilt 保留规则
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# 保留 Timber（用于 Release 日志）
-keep class timber.log.Timber** { *; }
# 移除 Debug 级别的 Timber 调用
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
}

# 移除 Kotlin 断言（Release 中不生效）
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkNotNullParameter(...);
    static void checkExpressionValueIsNotNull(...);
}

# 保留 Crashlytics 自定义日志方法
-keep class com.google.firebase.crashlytics.** { *; }
```

**R8 优化**：
- 启用 `isMinifyEnabled = true`
- 启用 `isShrinkResources = true`（移除未使用的资源，注意保留动态引用的资源）
- 启用 R8 完整模式（`android.enableR8.fullMode=true` 在 `gradle.properties` 中）
- Release 构建使用 `proguard-android-optimize.txt`（官方优化规则）而非 `proguard-android.txt`（仅混淆不优化）

### 3.2 反编译防护

| 措施 | 说明 | 优先级 |
|------|------|--------|
| R8 混淆 | 类名、方法名混淆为无意义字母 | 必须 |
| 资源压缩 | 移除未使用资源，压缩保留资源 | 必须 |
| 字符串加密 | 对关键字符串（API Key 片段）使用 DexGuard/iXGuard | 建议 |
| 代码虚拟化 | 将敏感逻辑编译为自定义虚拟机指令 | 按需（高安全场景） |
| 反调试 | 检测调试器附加并退出 | 按需（支付等高安全场景） |
| APK 完整性校验 | 检测 APK 是否被重新打包签名 | 建议 |

**字符串加密示例**（如使用 DexGuard）：

```kotlin
// 加密前
private const val SECRET = "dont_steal_this"
// 加密后（DexGuard 自动处理）
private val SECRET get() = "dont_steal_this".decrypt()  // 运行时动态解密
```

**实用建议**：
- Android 应用无法做到 100% 防反编译，目标是提高逆向成本。
- 核心策略：将敏感逻辑放在服务端，客户端仅做展示和交互。
- 定期使用 `jadx` / `apktool` 自行反编译 APK 检查暴露面。

### 3.3 敏感字符串

**禁止硬编码**：API Key、Secret、Token、服务端地址等不得以明文形式出现在代码中。

**环境信息管理方式**：

| 类型 | 方案 | 示例 |
|------|------|------|
| API Base URL | `BuildConfig`（来源 gradle 属性） | `BuildConfig.API_BASE_URL` |
| 第三方 SDK Key | `local.properties`（不进 Git）→ 注入 `BuildConfig` | `BuildConfig.THIRD_PARTY_KEY` |
| 地图/支付 Key | AndroidManifest `<meta-data>` + 构建变体覆盖 | 每个 variant 使用不同 manifest |
| Firebase 配置 | `google-services.json`（由 Firebase 控制台下载，不进 Git，CI 中通过 secret 下发） | `app/google-services.json` |

**敏感信息校验**：CI 中通过自定义脚本或 GitLeaks 扫描代码库，禁止提交以下模式：
- `= "sk-..."`（API Key 模式）
- `password = "..."`（明文字符串密码）
- `secret = "..."`（密钥赋值）
- Token 嵌入代码的模式 `"Bearer eyJ..."`

**安全提醒**：
- 即使 `local.properties` 在 `.gitignore` 中，也要避免在团队 IM/邮件中明文传递密钥。
- 密钥轮换周期：Token 的 Refresh Token < 30 天，第三方 API Key < 90 天。

---

## 4. 运行时安全

### 4.1 Root 检测

**检测策略**：多层综合判断，不依赖单一检测点。

```kotlin
object RootDetector {
    fun isDeviceRooted(): Boolean {
        return checkBuildTags() || checkSuperUserApk() || checkSuBinary() || checkMagisk()
    }

    // 检测 Build.TAGS 是否包含 "test-keys"（非官方签名）
    private fun checkBuildTags(): Boolean {
        return Build.TAGS.contains("test-keys")
    }

    // 检测是否安装了 Superuser/SuperSU 等 Root 管理应用
    private fun checkSuperUserApk(): Boolean {
        val rootApps = listOf(
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.topjohnwu.magisk"
        )
        return rootApps.any { pkg ->
            try {
                Runtime.getRuntime().exec(arrayOf("pm", "list", "packages", pkg))
                    .inputStream.bufferedReader().readText().contains(pkg)
            } catch (e: Exception) { false }
        }
    }

    // 检测 PATH 中是否存在 su 二进制
    private fun checkSuBinary(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
        )
        return paths.any { File(it).exists() }
    }

    // 检测 Magisk（最广泛使用的 Root 方案）
    private fun checkMagisk(): Boolean {
        return listOf(
            "/sbin/.magisk",
            "/data/adb/magisk",
        ).any { File(it).exists() }
    }
}
```

**响应策略**：
- Root 检测不应直接在启动时崩溃应用，应根据业务需要分级处理。
- 短剧播放场景：Root 设备仅记录标记（上报到 Firebase），不禁用功能。
- 支付场景：Root 设备阻止支付操作，提示"为保障支付安全，不支持在已 Root 设备上操作"。
- 检测结果缓存到内存（不持久化），避免每次重复检测。

### 4.2 模拟器检测

**检测策略**：综合多项指征判断。

```kotlin
object EmulatorDetector {
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK")
            || Build.MANUFACTURER.contains("Genymotion")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || "google_sdk" == Build.PRODUCT
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu")
        )
    }
}
```

**响应策略**（根据业务需要调整）：
- 模拟器仅用于开发和测试，生产环境模拟器用户标记为"非真实用户"。
- 不强退模拟器上的应用，但可能在风控系统中降低权重。
- 支付/提现功能禁止模拟器执行。

**注意**：模拟器检测不应作为唯一的安全手段，需配合服务端风控（如行为分析、设备指纹）使用。

### 4.3 截屏防护

**FLAG_SECURE**：防止敏感页面被截屏、录屏，以及出现在多任务预览中。

```kotlin
// 在需要保护的 Activity 中
class VideoPlaybackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 禁止截屏和录屏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        setContent { /* ... */ }
    }
}
```

**使用场景**：

| 场景 | FLAG_SECURE | 原因 |
|------|-------------|------|
| 视频播放页 | 建议开启 | 保护版权内容，防止盗录 |
| 支付页面 | 强制开启 | 防止支付信息泄露 |
| 登录/注册页 | 建议开启 | 防止密码被截图 |
| 个人资料页 | 可选 | 防止隐私信息泄露 |
| 首页 Feed | 不开启 | 允许用户分享截图（促进传播） |

**动态控制**：
```kotlin
// 播放受版权保护的独占内容时开启，普通内容不开启
fun setSecureFlag(enabled: Boolean) {
    if (enabled) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
```

**注意**：
- `FLAG_SECURE` 同时影响截屏、录屏和 Google Assistant "屏幕搜索"功能。
- Android 14+ 中 `FLAG_SECURE` 的行为更加严格，可能影响无障碍服务的截屏权限。
