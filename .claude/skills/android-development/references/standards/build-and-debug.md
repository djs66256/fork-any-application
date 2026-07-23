# 编译、运行与调试 — Android

> 本文档定义 Android 端的构建、运行与调试规范。

---

## 1. 构建系统

<!-- TODO: 补充 Gradle 配置 -->

### 1.1 Gradle 结构

<!-- TODO: 根 build.gradle.kts、settings.gradle.kts、模块 build.gradle.kts -->

### 1.2 Build Variants

<!-- TODO: debug / release / staging 配置 -->

### 1.3 签名配置

<!-- TODO: debug 签名、release 签名、keystore 管理 -->

### 1.4 依赖管理

<!-- TODO: Version Catalog (libs.versions.toml) 使用 -->

---

## 2. 编译命令

<!-- TODO: 补充常用 Gradle 命令 -->

### 2.1 编译

```bash
# TODO: assemble、bundle 命令
```

### 2.2 测试

```bash
# TODO: test、connectedAndroidTest 命令
```

### 2.3 代码检查

```bash
# TODO: lint、ktlint、detekt 命令
```

---

## 3. 运行与调试

### 3.1 模拟器

<!-- TODO: AVD 创建、启动参数、快照 -->

### 3.2 安装 APK

<!-- TODO: adb install、无线调试 -->

### 3.3 Logcat

<!-- TODO: 日志过滤、自定义 TAG、logcat 命令 -->

### 3.4 断点调试

<!-- TODO: Android Studio 调试配置、条件断点 -->

---

## 4. 性能分析

### 4.1 Android Studio Profiler

<!-- TODO: CPU、Memory、Network、Energy -->

### 4.2 Systrace / Perfetto

<!-- TODO: 系统级追踪 -->

### 4.3 Layout Inspector

<!-- TODO: Compose 重组检查 -->
