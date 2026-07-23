# CI/CD — iOS

> 本文档定义 iOS 端的持续集成与持续交付规范。

---

## 1. CI 流水线

<!-- TODO: 补充 CI 配置 -->

### 1.1 触发条件

<!-- TODO: PR、Push、Tag、定时 -->

### 1.2 流水线阶段

<!-- TODO: Lint → Test → Build → Archive -->

### 1.3 环境

<!-- TODO: macOS runner、Xcode 版本管理 -->

---

## 2. 自动测试

<!-- TODO: 补充自动测试配置 -->

### 2.1 单元测试

<!-- TODO: xcodebuild test 在 CI 中执行 -->

### 2.2 UI 测试

<!-- TODO: 模拟器测试执行 -->

---

## 3. 自动打包

### 3.1 Archive

<!-- TODO: xcodebuild archive、Fastlane gym -->

### 3.2 签名自动化

<!-- TODO: Fastlane match、证书管理 -->

---

## 4. 发布流程

### 4.1 TestFlight

<!-- TODO: 内部测试 → 外部测试 -->

### 4.2 App Store

<!-- TODO: App Store Connect 提审、元数据管理 -->

### 4.3 版本管理

<!-- TODO: CFBundleVersion、CFBundleShortVersionString 规则 -->
