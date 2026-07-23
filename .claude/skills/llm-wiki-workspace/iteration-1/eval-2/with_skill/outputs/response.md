# 播放器相关逻辑现状分析

## 总体结论

**播放器功能在所有端均尚未实现。** 当前项目处于非常早期的骨架阶段，各端仅搭建了应用壳（App Shell）展示应用名和版本号，没有任何实际的播放器代码。

## 查阅路径

按照 llm-wiki 查阅流程执行：

1. 读取 `wiki/index.md` -- 定位到播放器功能域
2. 读取 `wiki/features/video-player.md` -- 播放器功能文档（纯占位）
3. 读取各端 CLAUDE.md 和源文件 -- 验证代码实际状态
4. 搜索关键字 `player`、`video`、`播放` -- 确认零实现

## Wiki 文档现状

`wiki/features/video-player.md` 存在但内容全部为占位标记，核心问题：

| 章节 | 状态 |
|------|------|
| 入口与路由 | 所有端的源文件路径均标注为 `[待确认]` |
| 核心逻辑（播放流程） | 5 个步骤的源文件全部标注 `[待确认]` |
| 边界与异常处理 | 3 个场景的源文件全部标注 `[待确认]` |
| 多端实现 | Web/Android/iOS/Backend 核心组件/类均标注 `[待确认]` |
| API 接口 | `POST /api/player/start` 和 `POST /api/player/stop` 源文件标注 `[待确认]` |
| 状态管理 | 播放进度、播放状态的存储方式标注 `[待确认]` |
| 已知限制 | 明确注记"播放器各端代码尚未初始化" |

`wiki/index.md` 中播放器功能域的状态为 `📅 规划中`（全部四端）。

## 代码实际状态

### Web 端（`web/src/`）

文件清单：
- `app/layout.tsx` — 根布局，引入字体和全局样式
- `app/page.tsx` — 首页，仅展示应用名、版本、环境
- `lib/config.ts` — 配置管理，无播放器相关配置
- `lib/schemas.ts` — 仅定义 `DramaSchema`（短剧元数据），无播放器相关 Schema

**无播放器组件、无播放页面路由、无视频相关 hooks 或状态管理。**

### Backend 端（`backend/src/`）

文件清单：
- `app/api/health/route.ts` — 唯一 API 端点 `GET /api/health`
- `app/page.tsx` — 后端首页，展示服务信息和健康检查链接
- `lib/config.ts` — 配置管理
- `lib/schemas.ts` — 仅定义 `HealthResponseSchema`

**无任何 player/video API 路由。** 文档中设想的 `POST /api/player/start` 和 `POST /api/player/stop` 不存在。后端连 Drama 数据模型都未定义（仅 Web 端有）。

### Android 端（`android/`）

唯一的 Kotlin 源文件：
- `MainActivity.kt` — 一个 `HomeScreen()` Composable，居中显示 "ShortDrama" 和 "Version 0.1.0"

**无 PlayerActivity、无 ExoPlayer/Media3 集成、无视频播放相关代码。**

### iOS 端（`ios/`）

Swift 源文件：
- `ShortDramaApp.swift` — App 入口
- `ContentView.swift` — 使用 `Image(systemName: "play.tv")` 图标 + 应用名 + 版本号

**无 PlayerViewController、无 AVPlayer 集成、无视频播放相关代码。** `play.tv` 仅是 SF Symbol 图标名，不是播放器实现。

## 数据模型现状

仅 Web 端有 `DramaSchema`（`web/src/lib/schemas.ts`）：

```typescript
DramaSchema = z.object({
  id: z.string(),
  title: z.string().min(1),
  description: z.string(),
  coverUrl: z.string().url(),
  category: z.string(),
  episodeCount: z.number().int().positive(),
})
```

**缺失的关键模型：**
- 无 `Episode`（剧集/视频）模型 -- 没有 `videoUrl`、`duration` 字段
- 无播放进度模型
- 无播放器配置模型（倍速、画质等）

## 实现倍速功能的前提条件

要开始做播放器的倍速功能，需要先完成以下基础设施（按顺序）：

1. **数据模型** -- 定义 `Episode` 或 `Video` 模型，包含 `videoUrl`、`duration` 字段
2. **Backend API** -- 实现视频资源接口（至少需要返回视频地址的端点）
3. **各端播放器组件** -- Web（如 video.js / hls.js）、Android（如 ExoPlayer/Media3）、iOS（如 AVPlayer）
4. **播放控制 UI** -- 播放/暂停、进度条、倍速选择器等
5. **倍速功能** -- 在播放器核心就绪后才能添加

当前所有上述依赖均未实现，倍速功能需要从零开始搭建整个播放器链路。

## 参考文档

- Wiki 播放器文档：`wiki/features/video-player.md`（占位）
- Wiki 全局索引：`wiki/index.md`（播放器标记为 📅 规划中）
- 应用壳文档：`wiki/features/app-shell.md`（各端骨架已搭建）
- 数据模型文档：`wiki/features/data-models.md`（仅 Web 有 Drama，Backend 未同步）
- 产品信息：`PRODUCT.md`（ShortDrama 短剧平台）
