# Android 开源库选型

> 本文件列出 Android 端将使用或可能使用到的开源库，按功能领域分组。
> 标记说明：
> - ✅ 已选定 / 强烈推荐
> - 🔶 备选 / 待评估
> - ⚠️ 需用户确认后才能引入

---

## 网络

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| OkHttp + Retrofit | HTTP 客户端 + REST API 封装 | ✅ | 业界标准组合 |
| Kotlinx Serialization / Moshi | JSON 序列化 | 🔶 | Kotlinx Serialization 更原生；Moshi 配合 Retrofit 更成熟 |
| Coil | 图片加载与缓存 | ✅ | Kotlin 原生、Compose 集成好、轻量 |
| Coil Video / Coil GIF | GIF / 视频缩略图加载 | 🔶 | 按需引入 |

---

## 架构与 DI

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Hilt (Dagger) | 依赖注入 | ✅ | Google 官方推荐，与 Jetpack 生态集成 |

---

## UI

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Jetpack Compose | 声明式 UI 框架 | ✅ | 已确定的技术栈 |
| Compose Navigation | 路由导航 | ✅ | 官方方案 |
| Accompanist | Compose 补充组件 | 🔶 | 部分已合入官方 |
| Lottie Compose | Lottie 动画 | 🔶 | 奖励/加载等动效 |
| ExoPlayer + media3 | 视频播放 | ✅ | Google 官方、全功能 |
| Shimmer | 骨架屏加载 | 🔶 | 轻量 |

---

## 数据持久化

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Room | SQLite ORM | ✅ | Google 官方 |
| DataStore Preferences | KV 键值存储 | ✅ | 替代 SharedPreferences |
| EncryptedSharedPreferences | 加密 KV 存储 | 🔶 | 敏感配置项 |

---

## 工具

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Timber | 日志框架 | ✅ | 轻量、支持 Tree 扩展 |
| LeakCanary | 内存泄漏检测 | 🔶 | Debug 依赖 |
| StrictMode | 主线程违规检测 | ✅ | Android 内置 |

---

## 测试

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| JUnit 5 | 单元测试框架 | ✅ | |
| MockK | Kotlin Mock 框架 | ✅ | 比 Mockito 更适合 Kotlin |
| Turbine | Flow 测试 | ✅ | 简化 StateFlow/SharedFlow 测试 |
| Compose UI Test | Compose UI 测试 | ✅ | 官方测试库 |
| Paparazzi | 无设备快照测试 | 🔶 | 免模拟器渲染 Compose 截图 |
| Robolectric | Android 单元测试 | 🔶 | 模拟 Android 环境跑测试 |

---

## 性能与分析

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Firebase Crashlytics | 崩溃收集 | ✅ | 业界标准 |
| Firebase Performance | 性能监控 | 🔶 | 启动、网络延迟等 |
| Google ML Kit | 设备端 AI / OCR | 🔶 | 按需 |

---

## 多媒体

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| ExoPlayer (media3) | 视频播放 | ✅ | 竖屏短剧核心播放器 |
| Glide / Coil | 图片加载 | ✅ | 二选一，优先 Coil |
| CameraX | 相机能力 | 🔶 | 可能需要拍照/录制功能 |

---

## 推送与通知

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Firebase Cloud Messaging | 推送服务 | ✅ | 标准方案 |

---

## 其他

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| LicenseToolsPlugin / OSS Licenses | 开源许可证展示 | 🔶 | 合规需要 |
| Sentry | 错误追踪 | 🔶 | Firebase Crashlytics 替代方案 |
