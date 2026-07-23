# Wiki 路径与文档格式标准

## 目录结构

```
wiki/
├── CLAUDE.md                           # wiki 维护指引（要求先加载 llm-wiki skill）
├── index.md                            # 全局索引，链接到各子系统索引
├── features/                           # 功能文档
│   ├── index.md                        # 功能域索引
│   └── <feature-name>/                 # 每个功能一个子目录，kebab-case
│       ├── index.md                    # 功能文档（主文档）
│       └── ...                         # 子功能拆分文档（按需）
├── api/                                # API 文档
│   ├── index.md                        # API 总索引
│   └── <domain>.md                     # 按业务域组织的 API 定义
├── architecture/                       # 跨端架构文档
│   ├── index.md                        # 架构文档索引
│   └── <topic>.md                      # 架构专题
├── decisions/                          # 技术决策记录
│   ├── index.md                        # 决策索引（时间倒序）
│   └── <YYYY-MM-DD>-<title>.md         # 单条技术决策
└── revision/                           # 修订记录（不需要 index）
    └── <YYYY-MM-DD>-<变更简述>.md       # 每次修订的记录，kebab-case
```

> **索引规则**：`revision/` 除外的每个目录都必须有 `index.md` 作为索引文件。
> **CLAUDE.md**：`wiki/CLAUDE.md` 放置 wiki 维护指引，要求 agent 在操作 wiki 前必须加载 llm-wiki skill。

## 功能域名约定

以下为当前项目中已存在的功能域。新增功能域时按 kebab-case 命名，保持简洁。

| 功能域 | `feature-name` | 当前状态 | 说明 |
|--------|---------------|---------|------|
| 应用壳 | `app-shell` | 已实现 | 各端应用启动入口与基础项目骨架 |
| 健康检查 | `health-check` | 已实现 | 后端服务运行状态监控端点 |
| 数据模型 | `data-models` | 已实现 | 核心数据实体定义与 Zod Schema 校验 |
| 深链 | `deeplink` | 已实现 | 自定义 URL Scheme 唤起应用 |
| 播放器 | `video-player` | API 已实现 | 视频播放、控制与交互，核心功能模块 |

> 功能域名称使用 kebab-case。新增功能域时在此表补充（含当前状态列）。

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

### 功能文档章节顺序

功能文档按以下顺序组织章节（模板见 `assets/feature-template.md`）：

1. **功能概述** — 一句话描述 + 核心价值 + 覆盖端 + 当前状态
2. **入口与路由** — 各端进入路径、路由/deeplink、源文件
3. **核心逻辑** — 关键业务流程步骤（含源文件标注）、边界与异常处理
4. **多端实现** — 按 Web / Android / iOS / Backend 分别描述核心文件与实现要点
5. **API 引用** — 链接到 `wiki/api/`，不在此处重复定义 API
6. **状态管理** — 关键状态、存储方式、作用域
7. **依赖关系** — 内部依赖（功能间）+ 外部依赖（第三方服务），顶级章节
8. **已知限制** — 当前未实现的边界、技术债务
9. **修订历史** — 日期 + 变更摘要

> 各功能可按需增减子章节（如 `配置管理`），但顶级章节顺序应保持一致。

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

模板见 `assets/index-template.md`。全局索引不直接罗列所有功能链接，而是链接到各子系统索引：

```markdown
- [功能文档](./features/index.md) — 各功能域的业务逻辑、入口路由、多端实现与各端状态
- [API 文档](./api/index.md) — 按业务域组织的 RESTful API 定义
- [架构专题](./architecture/index.md) — 跨端架构设计、技术栈选型与系统总览
- [技术决策](./decisions/index.md) — 项目关键架构选型与技术决策记录
```

> 功能链接的完整列表维护在 `wiki/features/index.md` 中，避免双重维护。

### 各层索引

- `wiki/features/index.md` — 功能域列表，格式：`- [功能名](feature-name/index.md) — 简短说明`
- `wiki/api/index.md` — API 域列表，格式：`- [API 域](domain.md) — 包含的接口`
- `wiki/architecture/index.md` — 架构专题列表，格式：`- [专题名](topic.md) — 简短说明`
- `wiki/decisions/index.md` — 决策记录列表（时间倒序），格式：`- [YYYY-MM-DD 标题](YYYY-MM-DD-title.md) — 状态`

## 文档尾部标记

每篇 wiki 文档末尾必须包含维护声明：

```markdown
---
*本文档由 llm-wiki skill 自动维护。*
```
