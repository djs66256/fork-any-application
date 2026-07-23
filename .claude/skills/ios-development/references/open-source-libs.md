# iOS 开源库选型

> 本文件列出 iOS 端将使用或可能使用到的开源库，按功能领域分组。
> iOS 端优先使用 Apple 原生框架（SwiftUI、Combine、SwiftData 等），仅在原生能力不足时引入第三方。
> 标记说明：
> - ✅ 已选定 / 强烈推荐
> - 🔶 备选 / 待评估
> - ⚠️ 需用户确认后才能引入

---

## 网络

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Alamofire | HTTP 客户端 | ✅ | 业界标准 |
| Kingfisher | 图片加载与缓存 | ✅ | Swift 原生、SwiftUI 集成好 |
| Nuke | 图片加载与缓存 | 🔶 | 性能优异，API 设计现代 |
| Nuke Video | 视频缩略图 | 🔶 | 配合 Nuke |

---

## 架构与 DI

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Swinject | 依赖注入 | 🔶 | 功能全面 |
| TCA (The Composable Architecture) | 架构框架 | 🔶 | Point-Free 出品，学习中高 |

---

## UI

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| SwiftUI | 声明式 UI 框架 | ✅ | Apple 原生 |
| Lottie iOS | Lottie 动画 | 🔶 | 奖励/加载动效 |
| SwiftUIIntrospect | SwiftUI 底层修改 | 🔶 | 访问底层 UIKit 属性 |
| WrappingHStack | 流式布局 | 🔶 | SwiftUI 原生缺失 |

---

## 视频播放

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| AVPlayer (AVKit) | 视频播放 | ✅ | Apple 原生 |
| KSYMediaPlayer / ijkplayer | 第三方播放器 | 🔶 | 需要更细粒度控制时 |
| VideoPlayer (SwiftUI) | SwiftUI 视频封装 | ✅ | iOS 14+ 原生 |

---

## 数据持久化

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| SwiftData | ORM 框架 | 🔶 | iOS 17+，需评估用户覆盖率 |
| Core Data | 对象持久化 | ✅ | Apple 原生 |
| GRDB | SQLite 封装 | 🔶 | 比 Core Data 更灵活，纯 Swift |
| KeychainAccess | Keychain 封装 | ✅ | 敏感数据存储 |

---

## 工具

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| SwiftLint | 代码规范检查 | ✅ | 行业标准 |
| swift-log | 日志框架 | ✅ | Apple 官方 |
| CocoaLumberjack | 日志框架 | 🔶 | 功能更丰富 |
| DeviceKit | 设备信息 | 🔶 | 替代 UIDevice |

---

## 测试

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| XCTest | 测试框架 | ✅ | Apple 原生 |
| Swift Testing | 新一代测试框架 | 🔶 | Swift 6 新特性 |
| SwiftSnapshotTesting | 快照测试 | 🔶 | Point-Free 出品 |
| ViewInspector | SwiftUI View 测试 | 🔶 | 弥补 SwiftUI 测试困难 |
| Nimble | 断言库 | 🔶 | 更易读的断言 |

---

## 性能与分析

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Firebase Crashlytics | 崩溃收集 | ✅ | 业界标准 |
| MetricKit | 性能监控 | ✅ | Apple 原生 |
| Pulse | 网络日志/调试 | 🔶 | 调试期网络请求检视 |

---

## 多媒体

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| AVFoundation | 音视频框架 | ✅ | Apple 原生 |
| Core Image | 图像处理 | ✅ | Apple 原生 |
| Vision | 图像识别 | 🔶 | Apple 原生，按需 |

---

## 推送与通知

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| APNs (原生) | 推送服务 | ✅ | Apple 原生 |
| Firebase Cloud Messaging | 推送通道 | 🔶 | 跨平台统一推送 |

---

## 其他

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| LicensePlist | 开源许可证生成 | 🔶 | 合规需要 |
| Sentry | 错误追踪 | 🔶 | Crashlytics 替代方案 |
| Fastlane | 自动构建/发布 | ✅ | iOS CI/CD 标准工具 |
