# 编译、运行与调试 — Android

> 本文档定义 Android 端的构建、运行与调试规范。

---

## 1. 构建系统

使用 Gradle Kotlin DSL（`.kts`）管理构建配置。

### 1.1 Gradle 结构

```
android/
├── build.gradle.kts              # 根构建脚本：公共插件声明
├── settings.gradle.kts           # 模块注册、仓库配置、Version Catalog
├── gradle.properties             # JVM 参数、AndroidX 开关
├── gradle/
│   └── libs.versions.toml        # Version Catalog：统一管理依赖版本
└── app/
    └── build.gradle.kts          # App 模块构建脚本
```

**settings.gradle.kts 关键配置**：
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ShortDrama"
include(":app")
```

**根 build.gradle.kts**：
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
```

### 1.2 Build Variants

定义三种构建变体：

| Variant | 用途 | debuggable | minifyEnabled | API 地址 |
|---------|------|------------|---------------|----------|
| `debug` | 开发调试 | true | false | 测试环境 |
| `staging` | 内测灰度 | true | true | 预发布环境 |
| `release` | 正式发布 | false | true | 生产环境 |

**app/build.gradle.kts 配置**：

```kotlin
android {
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"https://test-api.shortdrama.example.com\"")
        }
        create("staging") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "API_BASE_URL", "\"https://staging-api.shortdrama.example.com\"")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "API_BASE_URL", "\"https://api.shortdrama.example.com\"")
        }
    }
}
```

### 1.3 签名配置

**Debug 签名**：使用 Android Studio 自动生成的 `debug.keystore`（`~/.android/debug.keystore`），不需要手动配置。

**Release 签名**：签名信息绝对不能提交到版本控制。

在 `android/keystore.properties`（已在 `.gitignore` 中）：
```properties
storeFile=../keystore/release.jks
storePassword=${SHORTDRAMA_STORE_PASSWORD}
keyAlias=shortdrama
keyPassword=${SHORTDRAMA_KEY_PASSWORD}
```

在 `app/build.gradle.kts` 中引用：
```kotlin
android {
    signingConfigs {
        val keystoreProperties = java.util.Properties()
        val keystoreFile = rootProject.file("keystore.properties")
        if (keystoreFile.exists()) {
            keystoreProperties.load(keystoreFile.inputStream())
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

CI 中通过环境变量传递密码：`${SHORTDRAMA_STORE_PASSWORD}`、`${SHORTDRAMA_KEY_PASSWORD}`。

### 1.4 依赖管理

使用 Gradle Version Catalog (`gradle/libs.versions.toml`) 统一管理依赖版本。

**libs.versions.toml 结构**：

```toml
[versions]
kotlin = "2.1.0"
compose-bom = "2025.02.00"
retrofit = "2.11.0"
okhttp = "4.12.0"
coil = "3.0.4"
room = "2.6.1"
hilt = "2.51.1"
lifecycle = "2.8.7"

[libraries]
# Kotlin
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version = "1.7.3" }
# Compose
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }
# Network
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }
# Image
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
# Database
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
# DI
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
# Test
junit5 = { module = "org.junit.jupiter:junit-jupiter", version = "5.11.0" }
mockk = { module = "io.mockk:mockk", version = "1.13.12" }
turbine = { module = "app.cash.turbine:turbine", version = "1.1.0" }

[plugins]
android-application = { id = "com.android.application", version = "8.7.3" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "2.1.0-1.0.29" }
```

**模块中使用**：
```kotlin
// app/build.gradle.kts
dependencies {
    implementation(libs.compose.bom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.retrofit)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // ...
}
```

**版本更新规则**：
- BOM 类依赖（Compose BOM）跟随官方发布节奏更新（每月一次）。
- 其他依赖每季度评估一次更新，避免频繁升级引入不兼容变更。
- 禁止在 `libs.versions.toml` 之外的地方定义版本号。

---

## 2. 编译命令

所有命令在 `android/` 目录下执行。

### 2.1 编译

```bash
# Debug APK（默认，含调试信息）
./gradlew assembleDebug
# 输出：app/build/outputs/apk/debug/app-debug.apk

# Release APK（混淆，不含调试信息）
./gradlew assembleRelease
# 输出：app/build/outputs/apk/release/app-release.apk

# Staging APK（带混淆，可调试）
./gradlew assembleStaging
# 输出：app/build/outputs/apk/staging/app-staging.apk

# Android App Bundle（正式发布用，Google Play 要求）
./gradlew bundleRelease
# 输出：app/build/outputs/bundle/release/app-release.aab

# 刷新依赖缓存
./gradlew dependencies --refresh-dependencies

# 显示所有可用的 Gradle 任务
./gradlew tasks --all

# 清理中间产物
./gradlew clean
```

### 2.2 测试

```bash
# 运行所有单元测试（JVM 环境，速度快）
./gradlew test

# 运行指定模块的单元测试
./gradlew :app:testDebugUnitTest

# 运行指定测试类
./gradlew :app:testDebugUnitTest --tests "com.djs66256.short_drama.domain.GetHomeFeedUseCaseTest"

# 生成测试覆盖率报告
./gradlew :app:testDebugUnitTest jacocoTestReport
# HTML 报告输出：app/build/reports/jacoco/jacocoTestReport/html/index.html

# 运行 Instrumentation 测试（需要模拟器或真机）
./gradlew connectedAndroidTest

# 运行指定 Instrumentation 测试
./gradlew :app:connectedDebugAndroidTest --tests "com.djs66256.short_drama.ui.HomeScreenTest"
```

### 2.3 代码检查

```bash
# ktlint 检查（不自动修复）
./gradlew ktlintCheck

# ktlint 自动格式化
./gradlew ktlintFormat

# detekt 静态分析
./gradlew detekt

# Lint 检查（Android Lint）
./gradlew lint

# Lint 报告
# HTML 报告：app/build/reports/lint-results-debug.html

# 一键执行全部检查
./gradlew ktlintCheck detekt lint test
```

---

## 3. 运行与调试

### 3.1 模拟器

**AVD 创建**：使用 `avdmanager` CLI 或 Android Studio AVD Manager。

```bash
# 列出可用的系统镜像
sdkmanager --list | grep "system-images"

# 下载推荐镜像（API 34, arm64-v8a, Google API）
sdkmanager "system-images;android-34;google_apis;arm64-v8a"

# 创建 AVD（首选硬件配置）
avdmanager create avd \
    -n shortdrama_api34 \
    -k "system-images;android-34;google_apis;arm64-v8a" \
    -d "pixel_6" \
    --force
```

**启动参数**：

```bash
# 启动模拟器
emulator -avd shortdrama_api34 -no-boot-anim -netdelay none -netspeed full

# 常用参数说明：
# -no-boot-anim    跳过启动动画（加速启动）
# -netdelay none   取消网络延迟模拟
# -netspeed full   全速网络
# -writable-system 可写系统分区
# -wipe-data       清除并重新初始化用户数据
# -gpu host        使用宿主机 GPU 加速
# -no-snapshot     不使用快照，冷启动
```

**验证连接**：
```bash
adb devices
# 预期输出：
# List of devices attached
# emulator-5554   device
```

### 3.2 安装 APK

```bash
# 安装 APK（覆盖安装）
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 仅安装测试 APK
adb install -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

# 卸载
adb uninstall com.djs66256.short_drama

# 保留应用数据卸载（仅 -k 标志）
adb uninstall -k com.djs66256.short_drama

# 无线调试（Android 11+）
# 1. 在设备上打开"开发者选项 → 无线调试"
# 2. 配对设备
adb pair <ip>:<pairing-port> <pairing-code>
# 3. 连接
adb connect <ip>:<connect-port>
```

**推送测试数据**：
```bash
# 向设备推送文件
adb push test_data.json /sdcard/Download/test_data.json

# 从设备拉取文件
adb pull /sdcard/Download/logs/ ./local_logs/

# 向 BroadcastReceiver 发送广播
adb shell am broadcast -a com.djs66256.short_drama.DEBUG_ACTION
```

### 3.3 Logcat

```bash
# 清除旧日志
adb logcat -c

# 实时查看所有日志（带时间戳）
adb logcat -v time

# 按 TAG 过滤（支持正则）
adb logcat -v time | grep -E "HomeVM|NetAuth|Perf"

# 按优先级过滤（*:W 表示 WARN 及以上）
adb logcat -v time *:W

# 只显示指定 PID 的日志
adb logcat -v time --pid=$(adb shell pidof com.djs66256.short_drama)

# 输出到文件（后台）
adb logcat -v time > crash_log.txt

# 格式化输出（Threadtime 格式，含线程信息）
adb logcat -v threadtime

# 录制日志到环形缓冲区（保留最近 16MB）
adb logcat -G 16M
adb logcat -v threadtime -f /sdcard/app_log.txt
```

**自定义 TAG 规范**：应用内日志使用 Team 统一的 TAG 前缀 `SD_`（ShortDrama）。

```kotlin
Timber.tag("SD_HomeVM").d("Page=$page")
Timber.tag("SD_NetAuth").i("Token refreshed")
```

**IDE 中快速过滤**：在 Logcat 面板中设置 filter：
```
tag:SD_ level:info package:com.djs66256.short_drama
```

### 3.4 断点调试

**Android Studio 调试配置**：

- **Debug 模式启动**：点击工具栏 Debug 按钮（或 `Shift+F9`），应用在 Debug 模式下启动，可在任意位置暂停。
- **附加调试器**：Run → Attach Debugger to Android Process → 选择进程。适用于应用已启动但需要后调的场景。
- **条件断点**：在代码行号处右键 → "Add Conditional Breakpoint" → 输入条件（如 `videoId == "123"`）。仅条件满足时暂停，避免循环中频繁中断。
- **日志断点（Logging Breakpoint）**："Suspend" 取消勾选，仅记录日志不暂停线程。适用于不中断执行流程的插桩。
- **依赖断点（Dependency Breakpoint）**：在一个断点上设置触发条件为"另一个断点已触发后"，实现链式断点。

**调试快捷键**：

| 操作 | Windows/Linux | macOS |
|------|--------------|-------|
| Step Over | F8 | F8 |
| Step Into | F7 | F7 |
| Step Out | Shift+F8 | Shift+F8 |
| Resume (继续执行) | F9 | F9 |
| Evaluate Expression | Alt+F8 | Option+F8 |

**Evaluate Expression**：断点时选中变量或表达式，`Option+F8`（macOS）打开求值窗口，可执行任意 Kotlin 表达式，如 `viewModel.uiState.value.videos.size`。

---

## 4. 性能分析

### 4.1 Android Studio Profiler

打开方式：View → Tool Windows → App Inspection → Profiler，或工具栏直接点击 "Profile 'app'" 按钮。

| 面板 | 监测内容 | 关注指标 |
|------|---------|---------|
| **CPU** | 方法调用耗时、线程状态 | 主线程占用率、compose 重组耗时方法 |
| **Memory** | 堆内存分配、对象数量、GC 事件 | 对象泄漏趋势、Bitmap 内存占用、GC 频率 |
| **Network** | 网络请求数量和大小 | 请求耗时、重复下载、未压缩资源 |
| **Energy** | CPU/网络/位置耗电 | 后台网络唤醒、WakeLock 持有时长 |

**CPU Profiler 实用技巧**：
- 选择 "Sample Java Methods"（轻量，影响小）而非 "Trace Java Methods"（完整，开销大）。
- 查看火焰图（Flame Chart）：从底部往上追溯调用链，颜色越深表示占用越多。
- 录制时点击"Record" → 操作 App → 点击"Stop"，再在时间线上选择感兴趣的时间范围。

**Memory Profiler 实用技巧**：
- 在关键路径前手动触发 GC（面板上的垃圾桶按钮），然后开始操作，观察内存上升后是否回落。
- 如果操作后内存持续升高不回落，可能存在内存泄漏。
- Allocations 记录可以帮助定位短时间内频繁创建的对象。

### 4.2 Systrace / Perfetto

用于系统级性能追踪（跨进程）。

**Systrace 命令行方式**：

```bash
# 录制 10 秒，追踪 gfx、view、wm 等类别
python systrace.py -t 10 -o trace.html gfx view wm am res dalvik

# 常用 categories：
# gfx    - 渲染帧信息
# view   - View 系统（Compose 也部分适用）
# wm     - Window Manager
# am     - Activity Manager
# sched  - 内核调度
```

**Perfetto（Systrace 的继任者）**：

```bash
# 录制 15 秒（推荐使用 Android Studio 内置的 Perfetto UI）
adb shell perfetto \
    -c - --txt \
    -o /data/misc/perfetto-traces/trace \
    <<EOF
buffers: { size_kb: 63488 }
duration_ms: 15000
data_sources: {
    config {
        name: "linux.ftrace"
        ftrace_config {
            ftrace_events: "sched/sched_switch"
            ftrace_events: "power/suspend_resume"
        }
    }
}
EOF

# 从设备拉取 trace 文件
adb pull /data/misc/perfetto-traces/trace perfetto_trace.perfetto-trace
# 打开 https://ui.perfetto.dev 拖入 .perfetto-trace 文件查看
```

### 4.3 Layout Inspector

**Compose 专用**：Android Studio → View → Tool Windows → Layout Inspector。

- 勾选 "Live Updates" 实时查看当前屏幕的 Composable 树。
- 点击任意 Composable 可查看其参数（Modifier、状态值、传参）。
- **重组计数**：在组件树中可看到每个 Composable 的重组次数和跳过次数，高重组次数的组件是优化目标。
- 点击 "Show Borders" 可显示 Composable 的布局边界，帮助定位不必要的嵌套。
- 选择单个 Composable 后，`command+click`（macOS）可直接跳转到源代码。

**命令行替代方案**（无 IDE 时）：

```bash
# Dump Compose 语义树（通过 UIAutomator）
adb shell uiautomator dump /sdcard/ui_dump.xml
adb pull /sdcard/ui_dump.xml

# 查看窗口信息
adb shell dumpsys window windows | grep -i "mCurrentFocus"
adb shell dumpsys activity top | grep "ACTIVITY"
```
