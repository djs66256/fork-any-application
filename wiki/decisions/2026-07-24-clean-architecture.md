# 2026-07-24 — 各端 Clean Architecture 分层架构

> 状态：已采纳
> 决策人：daniel
> 最后更新：2026-07-24

## 背景

项目涉及 4 个端（Backend / Web / iOS / Android），需要统一的分层架构约束，避免各端架构风格分裂。spec.md Section 4 定义了完整的架构蓝图。

## 决策

各端采用统一的 Clean Architecture 分层模型，遵循依赖倒置原则（上层依赖接口，下层提供实现）：

- **Backend**：四层（Route → Service → Repository → Infrastructure）+ Shared 层
- **Web**：五层（Page → Feature → Shared UI → Core → Design System）
- **iOS / Android**：三层（Presentation → Domain → Data）+ Core 层

各端核心约束：Domain 层零框架依赖（纯语言类型）；Repository Interface 在 Domain 层定义；Data 层实现接口；DI 框架在 Core 层提供。

## 备选方案

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| 统一 Clean Architecture | 依赖方向清晰；可测试性强（mock repository）；跨端开发经验可迁移 | 初始搭建成本高；代码量多 | 采纳 |
| 简单 MVC / 无分层 | 初始开发快 | 后期维护难；业务逻辑与 UI 耦合；跨端不一致 | 拒绝（不利于大型项目长期演进） |

## 影响

- Backend: Route 禁止直接调用 Repository，必须通过 Service 层
- iOS/Android: Domain 层禁止 import UIKit / android.*，可独立编译测试
- 各端 Repository Interface 在 Domain 层定义，Data 层提供具体实现
- Web: Feature 间禁止直接引用，通过 `lib/types.ts` 共享类型

### 源文件

- `docs/specs/2026-07-24-project-init/spec.md` Section 4（架构设计）
- `backend/src/services/` — Service 层实现
- `backend/src/repositories/interfaces/` — Repository Interface 定义
- `ios/ShortDrama/Sources/Domain/RepositoryProtocols/` — iOS Domain 层 Repository 协议
- `android/.../domain/repository/` — Android Domain 层 Repository 接口

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-24 | 初始创建，记录 Clean Architecture 分层决策 |

---

*本文档由 llm-wiki skill 自动维护。*
