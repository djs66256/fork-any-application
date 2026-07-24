# Init Project

## 定位

init-project 是「项目初始化引擎」——当需要 fork 一个现有应用并从头搭建工程基础设施时，编排从产品信息定义、monorepo 结构创建、各端工程脚手架、分层架构设计、CI/CD 到 wiki/docs 骨架的完整初始化流程。

它不替代 product-manager（产品决策）或 feature-workflow（开发落地），而是串联两者完成「从零到工程就绪」的首次初始化。

```
init-project
    ├── product-manager  →  PRD（项目初始化与架构设计）
    └── feature-workflow →  阶段 1-15（spec → design → plan → coding → wiki）
```

## 设计理念

init-project 解决的核心问题：**每次 fork 一个新应用时，初始化流程高度重复但涉及大量跨 skill 协作**。本 skill 将流程固化，减少重复决策，确保每次初始化产出的工程结构一致、架构规范统一。

## 触发时机

当用户表达以下意图时使用本 skill：

- "fork X 项目"、"复刻 X"、"copy X"
- "初始化项目"、"新建工程"、"项目搭建"
- "搭建 monorepo"、"多端工程初始化"
- "从零开始搭建 XX 应用"

即使没有明确提到"初始化"，只要用户意图是在开始业务功能开发前先建立工程基础设施，就应该使用本 skill。

## 工作流总览

```
Phase 1              Phase 2                Phase 3                Phase 4
产品信息定义      →  PRD 撰写            →  Spec & Design       →  Implementation
(PRODUCT.md)        (product-manager)      (feature-workflow)     (feature-workflow)
                                             阶段 1-8               阶段 9-15
```

| 阶段 | 职责 | 执行方式 | 产物 |
|------|------|---------|------|
| **Phase 1: 产品信息定义** | 收集竞品名、产品定位、技术标识，写入 PRODUCT.md | 主 agent 直接执行 | `PRODUCT.md` |
| **Phase 2: PRD 撰写** | 调用 product-manager 生成项目初始化的 PRD + 子任务拆分 | 通过 product-manager skill | `docs/product_manager/prd/<date>-project-init/prd.md` + `subtasks.md` |
| **Phase 3: Spec & Design** | 通过 feature-workflow 阶段 1-8：spec 撰写 → review → 人确认 → shared design → 各端 design → review → 人确认 | 通过 feature-workflow skill | spec.md、design.md、design-{platform}.md |
| **Phase 4: Implementation** | 通过 feature-workflow 阶段 9-15：各端 plan → coding → code review → 人确认 → QA → merge → wiki | 通过 feature-workflow skill | 各端代码、CI/CD 配置、wiki 文档 |

## 架构原则（全局约束）

初始化各端工程时，必须遵循以下架构原则（详见 [references/architecture-design.md](references/architecture-design.md)）：

| 原则 | 说明 |
|------|------|
| **分层解耦** | 每端内部按职责分层，上层依赖下层，禁止反向依赖 |
| **模块化** | 按业务域（feature）拆分模块，模块内部高内聚、模块间低耦合 |
| **依赖倒置** | 数据层定义 Repository 接口/协议，上层只依赖接口 |
| **可测试性** | 每层可独立单测，接口注入 + mock 实现 |
| **BaaS 优先** | Backend 优先使用 Supabase 作为基础服务（PostgreSQL、Auth、Storage、Realtime） |

## 各端技术栈（标准化选型）

| 层级 | Backend | Web | iOS | Android |
|------|---------|-----|-----|---------|
| 语言 | TypeScript 5.x | TypeScript 5.x | Swift 6 | Kotlin 2.0 |
| 框架 | Next.js 16 App Router | Next.js 16 + React 19 | SwiftUI | Jetpack Compose + Material3 |
| 数据校验 | Zod ≥ 4 | Zod ≥ 4 | — | — |
| 构建 | next build | next build | XcodeGen + Xcode 27 | AGP 8.x + Gradle KTS |
| 包管理 | npm | npm | SPM | Version Catalog |
| 架构 | Route→Service→Repository→Infrastructure | Page→Feature→SharedUI→Core→DS | Presentation→Domain→Data | Presentation→Domain→Data |
| BaaS | Supabase (DB/Auth/Storage/Realtime) | — | — | — |
| DI | 构造函数注入 | — | 构造函数注入 | Hilt/Koin |
| Lint | ESLint | ESLint | SwiftLint | Detekt |
| 测试 | Vitest | Vitest + Testing Library | XCTest | JUnit + MockK |

## 目录结构（标准化布局）

```
/
├── CLAUDE.md                  # 全局规则 + 目录职责 + 跨端约束
├── PRODUCT.md                 # 产品信息（名称、简介、竞品、技术标识）
├── .gitignore                 # 多端忽略规则
├── README.md                  # 项目简介 + 快速开始 + 目录导航
├── .github/
│   └── workflows/
│       └── ci.yml             # CI/CD 流水线
├── backend/
│   ├── CLAUDE.md              # Backend 开发规范
│   ├── src/
│   │   ├── app/api/           # Route 层
│   │   ├── services/          # Service 层
│   │   ├── repositories/      # Repository 层
│   │   ├── infrastructure/    # Supabase Client + Redis
│   │   └── lib/               # Shared 层（schemas, errors, config, types）
│   └── supabase/migrations/   # 数据库迁移
├── web/
│   ├── CLAUDE.md
│   └── src/
│       ├── app/               # Page 层
│       ├── features/          # Feature 层（按业务域）
│       ├── components/ui/     # Shared UI 层
│       ├── lib/               # Core 层
│       └── styles/            # Design System 层
├── ios/
│   ├── CLAUDE.md
│   └── ShortDrama/Sources/
│       ├── App/               # App 入口
│       ├── Core/              # Core 层（Network, Config, DesignSystem）
│       ├── Domain/            # Domain 层（Entity, UseCase, RepositoryProtocol）
│       ├── Data/              # Data 层（Repository, DataSource, DTO）
│       └── Features/          # Presentation 层（按业务域）
├── android/
│   ├── CLAUDE.md
│   └── app/src/main/java/<package>/
│       ├── core/              # Core 层（network, di, config, theme）
│       ├── domain/            # Domain 层（model, usecase, repository interface）
│       ├── data/              # Data 层（repository impl, datasource, dto）
│       └── feature/           # Presentation 层（按业务域）
├── wiki/                      # 知识库
│   ├── architecture/
│   ├── features/
│   ├── api/
│   └── decisions/
└── docs/                      # 项目文档
    ├── product_research/
    ├── product_manager/
    └── specs/
```

## 执行顺序（Phase 逐阶段执行）

### Phase 1: 产品信息定义

先执行，不依赖其他 skill。

1. 读取现有的 `PRODUCT.md`（如存在）
2. 根据用户描述的竞品和目标，填充或更新产品信息
3. 产物：`PRODUCT.md`（产品名称、简介、竞品列表、技术标识 appId/schema）

详见 [references/product-info.md](references/product-info.md)。

### Phase 2: PRD 撰写

调用 `product-manager` skill，传递用户意图。PRD 必须覆盖：

- 项目初始化和架构设计
- 各端工程脚手架
- CI/CD 流水线
- wiki/docs 骨架
- **关键**：PRD 中必须包含分层架构和模块化设计要求（见 [references/architecture-design.md](references/architecture-design.md)）

产物路径：`docs/product_manager/prd/<date>-project-init/prd.md` + `subtasks.md`

### Phase 3: Spec & Design

调用 `feature-workflow` skill，传递 PRD 路径。feature-workflow 的 spec-writing 阶段需要额外遵循架构约束（见 [references/architecture-design.md](references/architecture-design.md)），确保 spec 中定义了各端的分层目录结构和模块化策略。

本阶段覆盖 feature-workflow 阶段 1-8：worktree-setup → spec-writing → spec-review → spec-human-review → design-shared → design-platforms → design-review → design-human-review。

### Phase 4: Implementation

继续 feature-workflow 阶段 9-15：plan-platforms → coding-platforms → code-human-review → qa-blackbox-testing → worktree-merge → wiki-inclusion → completed。

各端 coding 阶段产出后，调用 product-manager 更新 progress 状态。

## 参考资源

### References

| 文件 | 用途 |
|------|------|
| [references/product-info.md](references/product-info.md) | 产品信息定义流程：PRODUCT.md 字段说明、竞品信息收集 |
| [references/monorepo-setup.md](references/monorepo-setup.md) | Monorepo 顶层结构搭建：CLUADE.md、.gitignore、README、目录创建 |
| [references/architecture-design.md](references/architecture-design.md) | 架构设计原则：各端分层规范、模块化策略、跨端对齐 |
| [references/platform-backend.md](references/platform-backend.md) | Backend 初始化：Supabase 集成、四层架构目录、API 骨架 |
| [references/platform-ios.md](references/platform-ios.md) | iOS 初始化：XcodeGen 配置、三层架构目录、SwiftLint |
| [references/platform-android.md](references/platform-android.md) | Android 初始化：Gradle 配置、三层架构目录、Detekt、Hilt |
| [references/platform-web.md](references/platform-web.md) | Web 初始化：Next.js 配置、五层架构目录、SSR 策略 |
| [references/cicd-setup.md](references/cicd-setup.md) | CI/CD 流水线：GitHub Actions 配置、paths filter、平台 job |
| [references/wiki-docs-setup.md](references/wiki-docs-setup.md) | Wiki 和 Docs 骨架：目录结构、索引文件、职责边界 |

### Assets（模板）

| 模板 | 用途 |
|------|------|
| `assets/claude-md-root-template.md` | 根目录 CLAUDE.md 模板 |
| `assets/product-md-template.md` | PRODUCT.md 模板 |
| `assets/gitignore-template.md` | 根目录 .gitignore 模板 |

## 关键约束

- **先 PRODUCT.md 后 PRD**：产品信息必须在 PRD 撰写前确定，避免 PRD 中硬编码产品名
- **PRD 必须包含架构设计**：在传递需求给 feature-workflow 前，PRD 中必须明确分层架构和模块化要求
- **Backend 使用 Supabase**：作为标准 BaaS 平台，包括数据库、认证、存储
- **遵循架构原则**：所有端的工程结构必须遵循分层解耦和模块化原则（见 [references/architecture-design.md](references/architecture-design.md)）
- **跨端命名一致**：相同业务域在各端使用一致的 feature 命名（如 Home、Player、DramaDetail）
- **禁止硬编码**：所有可配置值通过环境变量/配置文件注入
- **产物路径规范**：PRD 在 `docs/product_manager/`、spec/design 在 `docs/specs/`
- **不跳过确认点**：feature-workflow 的 3 个人工确认点必须完成
- **状态同步**：Phase 4 完成后，回到 product-manager 更新 progress.md

## 与其他 Skill 的关系

| Skill | 关系 | 调用时机 |
|-------|------|---------|
| product-manager | **串联** | Phase 2：生成 PRD + 子任务拆分 |
| feature-workflow | **串联** | Phase 3-4：spec → design → plan → coding → merge → wiki |
| product-research | **可选前置** | 如需竞品调研，在 Phase 1 前调用 |
| llm-wiki | **被 feature-workflow 内部调用** | feature-workflow 的 wiki-inclusion 阶段 |
