# 架构设计原则

> 面向大型软件开发的架构初始化规范。所有初始化项目必须遵循此规范。

## 架构总则

| 原则 | 说明 | 落地要求 |
|------|------|---------|
| **分层解耦** | 每端内部按职责分层，上层依赖下层，禁止反向依赖 | 目录结构天然体现分层 |
| **模块化** | 按业务域（feature）拆分模块，模块内部高内聚、模块间低耦合 | 各端 feature 目录独立，通过明确定义的接口通信 |
| **依赖倒置** | 高层模块不依赖低层实现，依赖抽象接口 | 数据层定义 Repository 接口/协议，上层只依赖接口 |
| **单一数据源 (SSOT)** | 每种数据只有一个权威来源，避免数据不一致 | Backend Schema 为权威来源，状态管理集中在 state 层 |
| **可测试性** | 每层可独立单测，不依赖真实外部服务 | 接口注入 + mock 实现；核心逻辑不依赖 UI 框架 |
| **显式依赖** | 模块依赖关系显式声明，避免隐式耦合 | 通过构造函数/参数注入，禁止全局单例滥用 |
| **BaaS 优先** | Backend 优先使用 Supabase 作为基础服务 | PostgreSQL + Auth + Storage + Realtime 统一通过 Supabase SDK 访问 |

---

## Backend 四层架构

```
Route 层 (src/app/api/**/route.ts)
  → Service 层 (src/services/**/*.service.ts)
    → Repository 层 (src/repositories/**/*.repository.interface.ts + *.repository.ts)
      → Infrastructure 层 (src/infrastructure/*.ts)
        + Shared 层 (src/lib/schemas.ts, errors.ts, types.ts, config.ts)
```

### 层间约束

| 约束 | 说明 |
|------|------|
| Route 层禁止直接调用 Repository | 必须通过 Service 层 |
| Service 层不依赖 HTTP 框架 | 输入输出为纯数据，可被 CLI/测试直接调用 |
| Repository 接口先行 | 每个 Repository 先定义 interface，再写 Supabase 实现 + Mock 实现 |
| Supabase Client 单一入口 | 通过 `infrastructure/supabase.ts` 导出单例 client 和 admin |

### 关键决策

| 决策 | 原因 |
|------|------|
| Supabase 作为 BaaS | 托管 PostgreSQL + Auth + Storage + Realtime，免运维 |
| Supabase Client 双实例 | `client`（anon key，受 RLS 限制）用于前端调用、`admin`（service role key，绕过 RLS）用于后端内部操作 |
| Migration 使用 Supabase CLI | `npx supabase migration new`，原生 SQL migration |
| 本地开发使用 `supabase start` | 替代手写 docker-compose.yml，一键启动完整 Supabase 栈 |
| Wrapper 函数替代 middleware.ts | Edge Runtime 限制无法访问 Node.js 依赖（Supabase SDK + Redis） |

---

## Web 前端五层架构

```
Page 层 (src/app/**/page.tsx)
  → Feature 层 (src/features/**/)       # 按业务域组织
    → Shared UI 层 (src/components/ui/)  # 跨 feature 复用
      → Core 层 (src/lib/)               # API client、config、schemas
        → Design System 层 (src/styles/)  # CSS tokens、主题
```

### 层间约束

| 约束 | 说明 |
|------|------|
| Feature 间禁止直接引用 | 不 import 其他 feature 的内部文件；公共类型通过 `lib/types.ts` 共享 |
| Page 只做组合 | Page 文件只 import feature 组件和 layout 组件，不写业务逻辑 |
| API client 统一出口 | 所有 HTTP 请求通过 `lib/api-client.ts` 发出 |
| Schema 以 Backend 为准 | `web/src/lib/schemas.ts` 手动对齐 Backend 的 Schema 定义 |

---

## iOS MVVM + Clean Architecture（三层）

```
Presentation 层 (Features/<Feature>/Views/ + ViewModels/)
  → Domain 层 (Domain/Entities/ + UseCases/ + RepositoryProtocols/)  # 纯 Swift，零框架依赖
    → Data 层 (Data/Repositories/ + DataSources/ + DTOs/)
      → Core 层 (Core/Network/ + Config/ + DesignSystem/)
```

### 层间约束

| 约束 | 说明 |
|------|------|
| Domain 层零依赖 | 不含 `import UIKit` / `import SwiftUI`，可独立编译和测试 |
| ViewModel 不持有 View | 通过 `@Published` 暴露状态，View 通过 `@StateObject` / `@ObservedObject` 订阅 |
| Repository 协议在 Domain 层定义 | Data 层仅实现协议，依赖方向：Data → Domain（依赖倒置） |
| Feature 间不直接引用 | 通过 Domain 层的 Entity 和 UseCase 通信 |

---

## Android MVVM + Clean Architecture（三层）

```
Presentation 层 (feature/<name>/ui/ + viewmodel/)
  → Domain 层 (domain/model/ + usecase/ + repository/)  # 纯 Kotlin，无 Android 依赖
    → Data 层 (data/repository/ + datasource/ + dto/)
      → Core 层 (core/network/ + di/ + config/ + theme/)
```

### 层间约束

| 约束 | 说明 |
|------|------|
| Domain 层纯 Kotlin | 不含 `import android.*`，可 JVM 单测 |
| ViewModel 使用 StateFlow | UI 状态通过 `StateFlow<UiState>` 暴露，不暴露 MutableStateFlow |
| DI 框架选型 | Hilt（推荐，Google 官方支持） |
| 单 Activity 架构 | 整个应用使用一个 MainActivity，通过 Compose Navigation 管理路由 |
| Deeplink 路由在 MainActivity | 解析 `intent.data` → 映射到 Compose Navigation 路由 path |

---

## 跨端模块映射

| 业务域 | Backend | Web | iOS | Android |
|--------|---------|-----|-----|---------|
| 首页 | `services/home/` | `features/home/` | `Features/Home/` | `feature/home/` |
| 播放器 | `services/player/` | `features/player/` | `Features/Player/` | `feature/player/` |
| 剧集详情 | `services/drama/` | `features/drama-detail/` | `Features/DramaDetail/` | `feature/dramadetail/` |
| 搜索 | `services/search/` | `features/search/` | `Features/Search/` | `feature/search/` |
| 用户 | `services/user/` | `features/user/` | `Features/User/` | `feature/user/` |

---

## Schema 对齐策略

| 层面 | 策略 |
|------|------|
| 数据模型定义 | Backend Zod Schema 为唯一权威来源 |
| Web 端对齐 | `web/src/lib/schemas.ts` 手动同步 Backend Schema 结构 |
| iOS 端对齐 | `Domain/Entities/` 中的 Swift struct 字段与 Backend Schema 一致（使用 `CodingKeys` 对齐 snake_case） |
| Android 端对齐 | `domain/model/` 中的 Kotlin data class 字段与 Backend Schema 一致（使用 `@SerialName` 对齐 snake_case） |
