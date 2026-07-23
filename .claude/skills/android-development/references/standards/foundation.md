# 基础库与基础能力 — Android

> 本文档定义 Android 端的基础库选型、集成方案与基础能力接入规范。

---

## 1. 网络层

<!-- TODO: 补充 HTTP 客户端配置 -->

### 1.1 Retrofit + OkHttp

<!-- TODO: 拦截器链、超时配置、日志拦截器 -->

### 1.2 序列化

<!-- TODO: Kotlinx Serialization / Moshi 配置 -->

### 1.3 API 响应模型

<!-- TODO: 统一响应包装、错误码映射 -->

---

## 2. 图片加载

<!-- TODO: 补充 Coil 配置 -->

### 2.1 配置项

<!-- TODO: 磁盘缓存、内存缓存、占位图 -->

### 2.2 Compose 集成

<!-- TODO: AsyncImage / rememberAsyncImagePainter 使用 -->

---

## 3. 数据持久化

### 3.1 Room

<!-- TODO: Entity 定义、DAO、Migration、类型转换器 -->

### 3.2 DataStore

<!-- TODO: Preferences DataStore vs Proto DataStore -->

---

## 4. 日志系统

<!-- TODO: 补充 Timber 配置 -->

### 4.1 日志级别

<!-- TODO: VERBOSE/DEBUG/INFO/WARN/ERROR 使用场景 -->

### 4.2 Release 日志

<!-- TODO: Release 构建日志裁剪、上报策略 -->

---

## 5. 性能监控

<!-- TODO: 补充性能监控方案 -->

### 5.1 启动耗时

<!-- TODO: 启动阶段定义、埋点方案 -->

### 5.2 卡顿监控

<!-- TODO: 主线程 Looper 监控、ANR 检测 -->

### 5.3 内存泄漏

<!-- TODO: LeakCanary 集成、常见泄漏模式 -->

---

## 6. 崩溃收集

<!-- TODO: 补充 Firebase Crashlytics 集成 -->

### 6.1 符号化

<!-- TODO: ProGuard Mapping 上传 -->

### 6.2 自定义日志

<!-- TODO: 崩溃前日志上下文 -->

---

## 7. 国际化 (i18n)

<!-- TODO: 补充多语言方案 -->

### 7.1 资源组织

<!-- TODO: values/values-zh/values-en 目录结构 -->

### 7.2 切换语言

<!-- TODO: 运行时语言切换方案 -->

---

## 8. 无障碍 (A11y)

<!-- TODO: 补充 TalkBack 适配规范 -->

### 8.1 contentDescription

<!-- TODO: 语义标签规范 -->

### 8.2 焦点顺序

<!-- TODO: Compose 焦点管理 -->
