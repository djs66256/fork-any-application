# ShortDrama Wiki Index

> 最后更新：2026-07-22
> 产品：ShortDrama（短剧内容平台）

## 功能域

| 功能域 | 文档 | Web | Android | iOS | Backend | 最后更新 |
|--------|------|-----|---------|-----|---------|----------|
| 应用壳 (App Shell) | [app-shell.md](features/app-shell.md) | ✅ | ✅ | ✅ | — | 2026-07-22 |
| 健康检查 (Health Check) | [health-check.md](features/health-check.md) | — | — | — | ✅ | 2026-07-22 |
| 数据模型 (Data Models) | [data-models.md](features/data-models.md) | 🚧 | — | — | 🚧 | 2026-07-22 |
| 深链 (Deeplink) | [deeplink.md](features/deeplink.md) | — | — | 📅 | — | 2026-07-22 |
| 播放器 | [video-player.md](features/video-player.md) | 📅 | 📅 | 📅 | 📅 | 2026-07-22 |
| 首页 Feed | 📅 待实施 | 📅 | 📅 | 📅 | 📅 | — |
| 搜索 | 📅 待实施 | 📅 | 📅 | 📅 | 📅 | — |
| 鉴权 | 📅 待实施 | 📅 | 📅 | 📅 | 📅 | — |
| 个人中心 | 📅 待实施 | 📅 | 📅 | 📅 | 📅 | — |
| 评论 | 📅 待实施 | 📅 | 📅 | 📅 | 📅 | — |
| 分享 | 📅 待实施 | 📅 | 📅 | 📅 | 📅 | — |
| 通知 | 📅 待实施 | 📅 | 📅 | 📅 | 📅 | — |
| 订阅/付费 | 📅 待实施 | 📅 | 📅 | 📅 | 📅 | — |
| 导航/路由 | 📅 待实施 | 📅 | 📅 | 📅 | 📅 | — |

> 状态标识：✅ 已完成 | 🚧 进行中 | 📅 规划中 | — 不适用
> 
> 📅 待实施 = 文档和代码均未实现，属完全空白功能域

## 各端技术栈

| 端 | 框架/语言 | 构建工具 | Bundle ID | 版本 |
|----|----------|---------|-----------|------|
| Web | Next.js 16, React 19, TS | next build | — | 0.1.0 |
| Backend | Next.js 16, TS, Zod | next build | — | 0.1.0 |
| Android | Kotlin 2.0.21, Compose, Material3 | AGP 8.7.0, Gradle | com.djs66256.short_drama | 0.1.0 |
| iOS | Swift 6, SwiftUI | XcodeGen, Xcode 27 | com.djs66256.short_drama | 0.1.0 |

## 最近更新

| 时间 | 功能域 | 变更摘要 |
|------|--------|---------|
| 2026-07-22 | 应用壳、健康检查、数据模型、深链 | 从各端源代码提取信息，首次生成功能文档 |
| 2026-07-22 | 播放器 | 播放器文档占位（各端代码尚未初始化，文档内容待代码实现后补充） |

## 架构专题

| 主题 | 文档 | 最近更新 |
|------|------|---------|
| — | — | — |

## 技术决策记录

| 日期 | 标题 | 文档 |
|------|------|------|
| — | — | — |

---

*本索引由 llm-wiki skill 自动维护。每次功能文档更新后同步刷新。*
