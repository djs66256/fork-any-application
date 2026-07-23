# CI/CD — Android

> 本文档定义 Android 端的持续集成与持续交付规范。

---

## 1. CI 流水线

<!-- TODO: 补充 CI 配置 -->

### 1.1 触发条件

<!-- TODO: PR、Push、Tag、定时 -->

### 1.2 流水线阶段

<!-- TODO: Lint → Test → Build → Archive -->

---

## 2. 自动测试

<!-- TODO: 补充自动测试配置 -->

### 2.1 单元测试

<!-- TODO: ./gradlew test 在 CI 中执行 -->

### 2.2 UI 测试

<!-- TODO: 模拟器/真机 farm 集成（Firebase Test Lab 等）-->

---

## 3. 自动打包

### 3.1 Debug 包

<!-- TODO: PR 自动构建 debug APK -->

### 3.2 Release 包

<!-- TODO: 签名自动化、环境配置切换 -->

### 3.3 AAB

<!-- TODO: Android App Bundle 构建 -->

---

## 4. 发布流程

### 4.1 内部测试

<!-- TODO: Firebase App Distribution / 内测分发 -->

### 4.2 Google Play

<!-- TODO: 内部测试 → 封闭测试 → 开放测试 → 正式发布 -->

### 4.3 版本管理

<!-- TODO: versionCode / versionName 规则 -->
