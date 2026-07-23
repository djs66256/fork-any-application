# 基础库与基础能力 — iOS

> 本文档定义 iOS 端的基础库选型、集成方案与基础能力接入规范。

---

## 1. 网络层

<!-- TODO: 补充 HTTP 客户端配置 -->

### 1.1 Alamofire / URLSession

<!-- TODO: 请求封装、拦截器、超时配置 -->

### 1.2 序列化

<!-- TODO: Codable、JSONDecoder 配置 -->

### 1.3 API 响应模型

<!-- TODO: 统一响应包装、错误码映射 -->

---

## 2. 图片加载

<!-- TODO: 补充 Kingfisher / Nuke 配置 -->

### 2.1 配置项

<!-- TODO: 磁盘缓存、内存缓存、占位图 -->

### 2.2 SwiftUI 集成

<!-- TODO: KFImage / NukeUI 使用 -->

---

## 3. 数据持久化

### 3.1 Core Data / SwiftData

<!-- TODO: Model 定义、Container、迁移 -->

### 3.2 Keychain

<!-- TODO: KeychainAccess 使用、敏感数据存储 -->

### 3.3 UserDefaults

<!-- TODO: @AppStorage 使用规范 -->

---

## 4. 日志系统

<!-- TODO: 补充 swift-log / CocoaLumberjack 配置 -->

### 4.1 日志级别

<!-- TODO: trace/debug/info/notice/warning/error/critical -->

### 4.2 Release 日志

<!-- TODO: Release 构建日志裁剪、上报策略 -->

---

## 5. 性能监控

<!-- TODO: 补充 MetricKit 集成 -->

### 5.1 启动耗时

<!-- TODO: 启动阶段定义（pre-main / post-main）-->

### 5.2 卡顿监控

<!-- TODO: Hang Detection -->

### 5.3 内存

<!-- TODO: Memory Graph、Leaks 检测 -->

---

## 6. 崩溃收集

<!-- TODO: 补充 Firebase Crashlytics 集成 -->

### 6.1 dSYM 上传

<!-- TODO: 符号化方案 -->

### 6.2 自定义日志

<!-- TODO: 崩溃前 breadcrumbs -->

---

## 7. 国际化 (i18n)

<!-- TODO: 补充多语言方案 -->

### 7.1 资源组织

<!-- TODO: xcstrings 管理 -->

### 7.2 切换语言

<!-- TODO: 运行时语言切换方案 -->

---

## 8. 无障碍 (A11y)

<!-- TODO: 补充 VoiceOver 适配规范 -->

### 8.1 accessibilityLabel

<!-- TODO: 语义标签规范 -->

### 8.2 accessibilityHint

<!-- TODO: 操作提示 -->

### 8.3 动态字体

<!-- TODO: Dynamic Type 适配 -->
