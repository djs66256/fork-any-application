# Wiki 路径与文档格式标准

## 目录结构

```
wiki/
├── index.md                           # 全局索引
├── features/                          # 功能文档
│   ├── index.md                       # 功能域索引
│   └── <feature-name>/                # 每个功能一个子目录，kebab-case
│       ├── index.md                   # 功能文档（主文档）
│       └── ...                        # 子功能拆分文档（按需）
├── api/                               # API 文档
│   ├── index.md                       # API 总索引
│   └── <domain>.md                    # 按业务域组织的 API 定义
├── architecture/                      # 跨端架构文档
│   ├── index.md                       # 架构文档索引
│   └── <topic>.md                     # 架构专题
├── decisions/                         # 技术决策记录
│   ├── index.md                       # 决策索引
│   └── <YYYY-MM-DD>-<title>.md        # 单条技术决策
└── revision/                          # 修订记录（不需要 index）
    └── <YYYY-MM-DD>-<变更简述>.md       # 每次修订的记录，kebab-case
```

> **索引规则**：`revision/` 除外的每个目录都必须有 `index.md` 作为索引文件。

## 功能域名约定

| 功能域 | `feature-name` | 说明 |
|--------|---------------|------|
| 首页 Feed | `homepage-feed` | 首页推荐流、加载更多、下拉刷新 |
| 播放器 | `video-player` | 视频播放、控制栏、倍速、横竖屏 |
| 搜索 | `search` | 搜索入口、结果展示、历史记录 |
| 鉴权 | `auth` | 登录、注册、token 管理 |
| 个人中心 | `user-profile` | 用户信息、设置、历史记录 |
| 评论 | `comments` | 评论列表、发表评论、回复 |
| 分享 | `share` | 分享面板、deeplink |
| 订阅/付费 | `subscription` | 会员、付费、订阅管理 |
| 通知 | `notifications` | 推送、站内信 |
| 导航/路由 | `navigation` | 页面跳转、deeplink、路由设计 |

> 功能域名称使用 kebab-case，保持简洁。新增功能域时在此表补充。

## 文档规范

各类型文档按对应模板创建和更新：

| 文档类型 | 模板 | 目录 |
|---------|------|------|
| 功能文档 | `assets/feature-template.md` | `wiki/features/<name>/index.md` |
| API 文档 | `assets/api-template.md` | `wiki/api/<domain>.md` |
| 架构文档 | `assets/architecture-template.md` | `wiki/architecture/<topic>.md` |
| 技术决策 | `assets/decisions-template.md` | `wiki/decisions/<YYYY-MM-DD>-<title>.md` |
| 全局索引 | `assets/index-template.md` | `wiki/index.md` |

> API 的完整定义统一维护在 `wiki/api/` 下，功能文档中通过链接引用，不在功能文档中重复定义 API。

## 拆分策略

当功能业务逻辑复杂、单篇文档难以承载时，agent 应自行判断并拆分为多文档。
**不要询问用户如何拆分**，按以下优先级自行决策：

**优先按子业务拆分**（当业务可自然拆分时）：

```
wiki/features/video-player/
├── index.md                  # 播放器概述与总索引
├── playback-core.md          # 核心播放逻辑
├── speed-control.md          # 倍速控制
├── gestures.md               # 手势交互
└── subtitles.md              # 字幕
```

**按端（职能）拆分**（当业务不可拆分但跨端实现差异大时）：

```
wiki/features/auth/
├── index.md                  # 鉴权概述与总索引
├── backend.md                # 后端鉴权逻辑
├── web.md                    # Web 端鉴权实现
├── android.md                # Android 端鉴权实现
└── ios.md                    # iOS 端鉴权实现
```

## 索引维护

所有索引文件必须保持最新。每个目录（除 `revision/`）都有独立的 `index.md`。

### 全局索引 `wiki/index.md`

模板见 `assets/index-template.md`，只保留功能链接列表，格式：

```markdown
- [功能名](./features/feature-name/index.md) — 简短说明
```

### 各层索引

- `wiki/features/index.md` — 功能域列表
- `wiki/api/index.md` — API 域列表
- `wiki/architecture/index.md` — 架构专题列表
- `wiki/decisions/index.md` — 决策记录列表（时间倒序）

### 各端状态标识

每个功能域在索引中标注各端实现状态：

| 状态 | 说明 |
|------|------|
| ✅ 已完成 | 该端已实现并记录在 wiki |
| 🚧 进行中 | 正在进行中 |
| 📅 规划中 | 已规划但尚未开始 |
| — 不适用 | 该功能不涉及此端 |
