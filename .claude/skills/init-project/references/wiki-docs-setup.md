# Wiki 和 Docs 骨架设置

## Wiki 目录结构

```
wiki/
├── CLAUDE.md                  # wiki 维护约定（由 AI 自动维护）
├── index.md                   # 全局功能索引
├── architecture/
│   ├── index.md               # 架构文档索引
│   └── overview.md            # 系统总览（初始骨架：技术栈、分层图）
├── features/
│   ├── index.md               # 功能域索引
│   ├── app-shell/
│   │   └── index.md           # 应用壳（各端入口、路由、配置）
│   ├── video-player/
│   │   └── index.md           # 播放器（骨架，标注"待开发"）
│   ├── data-models/
│   │   └── index.md           # 数据模型（Drama/Episode/User Schema）
│   └── deeplink/
│       └── index.md           # 深链（iOS URL Scheme + Android Deep Links）
├── api/
│   └── index.md               # API 文档索引
└── decisions/
    └── index.md               # 技术决策索引（含 ADR 模板）
```

## Docs 目录结构

```
docs/
├── README.md                  # docs 目录说明（用途、格式约定、职责边界）
├── product_research/
│   └── index.md               # 竞品调研文档索引
├── product_manager/
│   ├── roadmap.md             # 产品路线图
│   ├── backlog.md             # 功能待办池
│   ├── progress.md            # 项目进展
│   ├── decisions/             # 分析决策记录
│   ├── prd/                   # PRD 文档（按日期+功能组织）
│   └── revisions/             # 变更记录
│       └── revisions.md
└── specs/                     # 功能规格（由 feature-workflow 生成）
    └── README.md
```

## 职责边界

| 目录 | 职责 | 内容类型 |
|------|------|---------|
| `wiki/` | 项目「当前状态」的知识沉淀 | 架构、功能实现、API 定义、技术决策 |
| `docs/product_research/` | 竞品「做了什么」的分析 | 竞品功能、交互流程、业务逻辑 |
| `docs/product_manager/` | 产品「下一步做什么」的决策 | PRD、路线图、backlog、进展 |
| `docs/specs/` | 功能「怎么实现」的规格 | spec、design、plan（由 feature-workflow 产出） |

## 初始内容

每个 index.md 初始包含：
- 目录用途说明
- 导航链接（指向子文档或上级）
- "暂无文档，待后续补充"（空目录）
