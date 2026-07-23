# 播放器相关逻辑分析报告

## 总体结论

**当前项目中的播放器功能尚未实际实现。** 播放器在整个代码仓库中处于"规划中"阶段，仅有文档层面的设计记录，各端（Web、Android、iOS、Backend）均无播放器相关代码。

---

## 一、播放器相关文件全景

### 文档层

| 文件 | 内容 | 状态 |
|------|------|------|
| `wiki/features/video-player.md` | 播放器功能文档（由 llm-wiki 维护） | 文档占位，核心逻辑、多端实现、源文件引用均为"待确认" |
| `docs/api/player.md` | 播放器 API 接口文档 | 接口字段待定，仅规划了路径结构 |
| `wiki/index.md` | Wiki 索引 | 播放器标记为 📅 规划中，所有端均为 📅 |
| `PRODUCT.md` | 产品信息 | 定义了产品名 ShortDrama、appId、URL Scheme |

### 后端代码层

| 文件 | 内容 | 与播放器关系 |
|------|------|-------------|
| `backend/src/app/api/health/route.ts` | 健康检查 API（GET /api/health） | 无直接关系，是当前后端唯一实现的 API 路由 |
| `backend/src/lib/schemas.ts` | Zod Schema 定义（仅 HealthResponseSchema） | 无播放器相关 Schema |
| `backend/src/lib/config.ts` | 后端配置（APP_NAME/APP_VERSION/NODE_ENV） | 通用配置，无播放器特有配置 |
| `backend/src/app/page.tsx` | 后端首页占位页 | 仅展示服务名和版本 |

**`backend/src/app/api/player/` 目录不存在**，这意味着播放器相关的两个 API 端点（`POST /api/player/start`、`POST /api/player/stop`）均未实现代码。

### 前端 Web 代码层

| 文件 | 内容 | 与播放器关系 |
|------|------|-------------|
| `web/src/app/page.tsx` | Web 首页占位页 | 展示应用名和版本，无播放器入口 |
| `web/src/lib/schemas.ts` | Zod Schema 定义（仅 DramaSchema） | 定义了 Drama 数据模型（id/title/description/coverUrl/category/episodeCount），可作为播放器数据基础 |
| `web/src/lib/config.ts` | 前端配置 | 通用配置，无播放器特有配置 |

**Web 端无任何播放器组件、页面路由或 hooks 实现。**

### Android 代码层

| 文件 | 内容 | 与播放器关系 |
|------|------|-------------|
| `android/.../MainActivity.kt` | 入口 Activity + HomeScreen Composable | 仅展示 "ShortDrama" 标题和版本号 |
| `android/.../AndroidManifest.xml` | 应用清单 | 仅声明 LAUNCHER Activity，无 PlayerActivity |

**Android 端无播放器 Activity、无播放器 Composable、无视频播放依赖。**

### iOS 代码层

| 文件 | 内容 | 与播放器关系 |
|------|------|-------------|
| `ios/.../ContentView.swift` | 首页 View | 仅展示应用名、版本号和一个 play.tv 图标 |
| `ios/.../ShortDramaApp.swift` | 应用入口 | 加载 ContentView，无路由配置 |

**iOS 端无播放器 ViewController/View、无 AVPlayer 使用代码。**

---

## 二、播放器设计规划（仅文档层面）

### 规划的 API 接口

根据 `docs/api/player.md` 和 `wiki/features/video-player.md`：

| 方法 | 路径 | 说明 | 当前状态 |
|------|------|------|---------|
| POST | `/api/player/start` | 开始播放视频（旧路径 `/api/video/play`） | 路由文件不存在，请求/响应字段待定 |
| POST | `/api/player/stop` | 停止播放并上报播放进度 | 路由文件不存在，请求/响应字段待定 |

路径已于 2026-07-22 从 `/api/video/play` 变更为 `/api/player/start`。

### 规划的业务流程

1. 用户点击视频卡片 -> 跳转播放器页面（路由待实现）
2. 前端发起 `POST /api/player/start`（API 待实现）
3. Backend 返回视频流信息（处理逻辑待实现）
4. 前端初始化播放器开始播放（播放器组件待实现）
5. 退出时发送 `POST /api/player/stop`（上报逻辑待实现）

### 规划的边界处理

- 视频加载失败：显示错误提示 + 重试入口
- 网络中断：暂停播放，恢复后提示继续
- 视频资源不存在：返回错误信息 + 友好提示

---

## 三、项目当前整体状态

来自 `wiki/index.md` 的完整功能矩阵：

| 功能域 | Web | Android | iOS | Backend |
|--------|-----|---------|-----|---------|
| 应用壳 | ✅ | ✅ | ✅ | — |
| 健康检查 | — | — | — | ✅ |
| 数据模型 | 🚧 | — | — | 🚧 |
| 深链 | — | — | 📅 | — |
| **播放器** | **📅** | **📅** | **📅** | **📅** |
| 首页 Feed | 📅 | 📅 | 📅 | 📅 |
| 搜索 | 📅 | 📅 | 📅 | 📅 |
| 鉴权 | 📅 | 📅 | 📅 | 📅 |
| 个人中心 | 📅 | 📅 | 📅 | 📅 |
| 评论 | 📅 | 📅 | 📅 | 📅 |

唯一已实现的功能是应用壳（各端占位页）和健康检查 API。

---

## 四、实现倍速功能需要做的工作范围

基于以上分析，倍速功能目前没有任何基础实现。要完成这个功能，至少需要：

### 1. 后端（Backend）

- **新建** `backend/src/app/api/player/start/route.ts`：实现 POST /api/player/start
- **新建** `backend/src/app/api/player/stop/route.ts`：实现 POST /api/player/stop  
- **扩展** `backend/src/lib/schemas.ts`：添加播放器相关 Zod Schema（PlayStartRequest、PlayStopRequest 等）
- 倍速功能本身不直接需要后端新增接口（倍速是纯前端播放控制），但播放器 API 骨架必须先建好

### 2. 前端（Web）

- **新建播放器页面/组件**：视频播放器组件，内容渲染、播放控制 UI
- **新建路由**：`/play/:videoId` 页面
- **实现播放速度控制**：在播放器组件中添加倍速切换逻辑（0.5x/1.0x/1.25x/1.5x/2.0x 等）
- **状态管理**：播放状态（播放/暂停/加载中）、当前倍速、播放进度

### 3. 数据模型（Web + Backend）

- 当前 Web 端已有 `DramaSchema`（有 episodeCount 字段），但缺少单集视频（Episode）的数据模型
- 需要定义 Episode Schema（id、dramaId、title、videoUrl、duration 等）

### 4. Android / iOS

- 实现原生视频播放器组件
- 在播放器 UI 中添加倍速控制
- 接入后端 API

---

## 五、关键技术信息汇总

**技术栈**：
- Web 端：Next.js 16、React 19、TypeScript、Zod
- Backend：Next.js 16、TypeScript、Zod、Supabase（已声明但未使用）
- Android：Kotlin 2.0.21、Jetpack Compose、Material3
- iOS：Swift 6、SwiftUI

**命名规范**：
- 代码路径使用自然语言（如 `player` 而非拼音或缩写）
- API 设计遵循 RESTful 风格
- Schema 使用 Zod 校验

**约束**：
- 禁止硬编码常量
- 各端代码只能修改对应目录
- 新增开源依赖需征得用户同意
