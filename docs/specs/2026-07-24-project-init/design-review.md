# 技术方案 Review：项目初始化与架构设计

> Review 日期：2026-07-24
> Review 循环：第 1 轮
> 审查者：AI Agent

## 审查结果总览

### Shared 设计 (design.md)

| 维度 | 检查项数 | 通过 | 问题 | 已修复 |
|------|---------|------|------|--------|
| 与 Spec 一致性 | 0 | 0 | 0 | — |
| 功能完整性 | 0 | 0 | 0 | — |
| API 完整性 | 0 | 0 | 0 | — |
| 数据模型一致性 | 0 | 0 | 0 | — |
| 边界与错误处理 | 0 | 0 | 0 | — |
| 安全考虑 | 0 | 0 | 0 | — |
| 性能考虑 | 0 | 0 | 0 | — |

> **结论**：design.md 和 spec.md 均不存在，Shared 设计维度的所有审查项目无法执行。

### 平台设计 (design-{platform}.md)

| 平台 | 与 Spec 一致性 | 功能完整性 | 架构 | 文件变更 | API 调用 | 状态管理 | 测试策略 | 边界/错误处理 | 总体 |
|------|--------------|----------|------|---------|---------|---------|---------|-------------|------|
| Backend | ❌ 无spec | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ 部分满足 | ⚠️ 有遗漏 | ⚠️ |
| iOS | ❌ 方案缺失 | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Android | ❌ 方案缺失 | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Web | ❌ 无spec | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ 有遗漏 | ⚠️ |

## 发现的问题

### 方案交付完整性（全局）

#### 问题 G-1：spec.md 不存在，无法验证任何方案与需求的一致性

- **严重程度**：🔴 阻塞
- **维度**：与 Spec 一致性
- **描述**：`docs/specs/2026-07-24-project-init/spec.md` 文件不存在。根据审查流程，spec.md 是 design 阶段的输入，没有它，所有「与 Spec 一致性」维度的审查都无法执行。同时 design-backend.md 和 design-web.md 开头都声明了「对应 PRD：US-XX」和「对应需求：spec.md（Section X.X）」，但这些引用无法验证。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 G-2：design.md（共享技术方案）不存在

- **严重程度**：🔴 阻塞
- **维度**：API 完整性 / 数据模型一致性 / 跨端一致性
- **描述**：`docs/specs/2026-07-24-project-init/design.md` 文件不存在。design-backend.md 和 design-web.md 多处引用 design.md：
  - design-backend.md 开头声明「关联设计：design.md（跨端共享设计）」
  - design-backend.md Section 15「API 与 design.md 对齐」对比了 data model 和 endpoint
  - design-web.md Section 11「跨端共享逻辑落地」列出了与 design.md 的对应关系
  - 两端的 HealthResponseSchema、DramaSchema 定义不一致，缺乏 design.md 作为权威来源来 reconcile
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 G-3：design-ios.md 和 design-android.md 缺失

- **严重程度**：🔴 阻塞
- **维度**：平台完整性
- **描述**：iOS 和 Android 工程已经存在且包含代码（`ios/ShortDrama/Sources/` 下有 `ShortDramaApp.swift` 和 `ContentView.swift`，`android/app/src/main/java/` 下有 `MainActivity.kt`），但这两端的技术方案文档均未产出。如果 spec.md 中有涉及 iOS/Android 的用户故事，本应在本轮完成对应的方案设计。
- **修复状态**：❌ 未修复
- **修复说明**：—

### Backend 设计问题

#### 问题 Backend-1：DramaSchema 与 Web 端不一致，且缺乏 design.md 权威定义

- **严重程度**：🔴 阻塞
- **平台**：backend
- **维度**：数据模型一致性
- **描述**：Backend 的 DramaSchema（Section 5.1）定义了 12 个字段（id, title, description, coverUrl, category, episodeCount, tags, rating, createdAt, updatedAt），而 Web 的 DramaSchema（Section 10.2）仅定义了 6 个字段（id, title, description, coverUrl, category, episodeCount）。两者在字段集合、约束（Web 的 episodeCount 用 `positive()`，Backend 用 `nonnegative()`）上不统一。需要一个 design.md 作为权威的共享数据模型来源。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Backend-2：HealthResponseSchema 扩展未与 Web 端同步

- **严重程度**：🟡 关注
- **平台**：backend
- **维度**：数据模型一致性
- **描述**：Backend 在 Section 5.1 中将 HealthResponseSchema 从原有 3 个字段（status, timestamp, version）扩展为 5 个字段（增加 database, redis）。但 Web 端的 HealthResponseSchema（Section 10.2）仍保持原有 3 个字段定义。两端如果不一致，Web 端调用 `/api/health` 时 Zod 校验会失败（或丢弃新增字段）。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Backend-3：`/api/player/start` 和 `/api/player/stop` 不符合 RESTful 规范

- **严重程度**：🟡 关注
- **平台**：backend
- **维度**：RESTful 合规
- **描述**：CLAUDE.md 和 backend/CLAUDE.md 都明确要求「API 使用 RESTful 设计」。但 `POST /api/player/start` 和 `POST /api/player/stop` 是 RPC 风格的动词路径（start、stop），不符合 RESTful 的名词层级范式。建议改为：
  - `POST /api/play-sessions`（创建播放会话，对应 start）
  - `PATCH /api/play-sessions/{sessionId}`（更新播放位置，对应 stop/tracking）
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Backend-4：config.ts 中存在硬编码回退常量

- **严重程度**：🟡 关注
- **平台**：backend
- **维度**：安全与性能
- **描述**：CLAUDE.md 明确要求「禁止硬编码常量」。Section 3.2 的 config.ts 代码中包含以下硬编码回退值：
  - `database.url`: `'postgresql://postgres:postgres@localhost:5432/postgres'`
  - `redis.url`: `'redis://localhost:6379'`
  - `supabase.url`: `'http://localhost:8000'`
  
  虽然注释说「仅用于本地开发便利，生产环境必须通过环境变量显式注入」，但根据 CLAUDE.md 规范，这些默认值本身就是硬编码常量。建议：要么移除所有回退值（强制环境变量注入），要么将开发默认值集中到 `.env.example` 中并通过 dotenv 加载。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Backend-5：.env 示例中包含测试用途的 Supabase Key

- **严重程度**：🟡 关注
- **平台**：backend
- **维度**：安全与性能
- **描述**：Section 3.1 的 `.env` 示例中包含了 `SUPABASE_ANON_KEY` 和 `SUPABASE_SERVICE_ROLE_KEY` 的具体值（虽然是 demo 用途的 JWT token）。虽然是测试用途，但在公开仓库中暴露任何形式的 key（即使是 demo key）可能被误用。建议使用占位符 `your-anon-key-here` 或说明从 Supabase 控制台获取。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Backend-6：骨架端点未使用 withErrorHandler 包装

- **严重程度**：🟡 关注
- **平台**：backend
- **维度**：边界与错误处理
- **描述**：Section 7.3.3 的骨架路由示例直接调用 `formatErrorResponse(Errors.notImplemented(...))` 并返回，但没有用 Section 6.3 定义的 `withErrorHandler` wrapper 包裹。如果未来在骨架中添加其他逻辑时抛出非 AppError，将不会被统一错误处理器捕获。建议所有 Route handler 统一使用 `withErrorHandler`。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Backend-7：错误码定义不完整

- **严重程度**：🟡 关注
- **平台**：backend
- **维度**：边界与错误处理
- **描述**：Section 5.2 的 ErrorCode 仅定义了 5 种（NOT_FOUND, VALIDATION_ERROR, INTERNAL_ERROR, NOT_IMPLEMENTED, SERVICE_UNAVAILABLE）。但后续业务发展必然需要的错误码缺失：
  - `UNAUTHORIZED`（401）：Supabase Auth 集成后会需要
  - `FORBIDDEN`（403）：权限不足
  - `CONFLICT`（409）：并发写入冲突
  - `TOO_MANY_REQUESTS`（429）：限流
  - `BAD_REQUEST`（400）：除校验外的通用请求错误
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Backend-8：未涉及并发控制与幂等性设计

- **严重程度**：🟡 关注
- **平台**：backend
- **维度**：边界与错误处理
- **描述**：Section 7 的 API 设计中未提及：
  - 并发请求冲突的处理策略（如同时多个 POST 创建同名 Drama）
  - 幂等性保证（`POST /api/dramas` 重复提交的防护）
  - 分页参数边界值处理（page <= 0, pageSize > 最大限制）
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Backend-9：缺少限流策略

- **严重程度**：🟡 关注
- **平台**：backend
- **维度**：安全与性能
- **描述**：设计中未提及任何 API 限流（rate limiting）策略。考虑到这是面向公开使用的产品，生产环境需要限流保护。当前 CORS 设置为 `*`（全开放），如果未来对外暴露，缺乏限流可能导致服务被滥用。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Backend-10：测试策略未满足后端测试要求

- **严重程度**：🟡 关注
- **平台**：backend
- **维度**：测试策略覆盖
- **描述**：backend/CLAUDE.md 要求「需要编写单元测试」「涉及业务逻辑、参数校验、数据转换的改动，应同步补齐对应测试」。当前方案：
  - HealthService 有 2 个可执行测试用例（✅）
  - 其余 3 个 Service 测试文件仅为 `it.todo(...)` 占位（❌）
  - Repository、Route 的集成测试全部延迟（❌）
  
  即使当前 Service 为骨架，也应补充基本的参数校验和错误场景测试（如 getById 传入非法 id 格式是否抛出正确错误）。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Backend-11：CORS 配置过于宽松

- **严重程度**：🟢 建议
- **平台**：backend
- **维度**：安全与性能
- **描述**：Section 6.1 的 CORS 中间件设置 `Access-Control-Allow-Origin: *`。方案说明「当前开发阶段 CORS 全开放，后续接入真实 Supabase Auth 时收缩」，这本身是合理的。但建议在方案中明确写出后续将收缩到哪些域名（如 `localhost:3000` for Web dev），避免遗忘。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Backend-12：缺少优雅关闭（graceful shutdown）策略

- **严重程度**：🟢 建议
- **平台**：backend
- **维度**：边界与错误处理
- **描述**：Section 4.1 的 `closePool` 和 Section 4.2 的 `closeRedis` 函数已定义，但方案中未说明何时调用它们（SIGTERM/SIGINT handler）。建议增加 process signal handling 的描述，确保后端关闭时正确释放数据库连接和 Redis 连接。
- **修复状态**：❌ 未修复
- **修复说明**：—

### Web 设计问题

#### 问题 Web-1：DramaSchema 与 Backend 端不一致

- **严重程度**：🔴 阻塞
- **平台**：web
- **维度**：数据模型一致性
- **描述**：与 Backend-1 对应。Web 的 DramaSchema 仅 6 个字段且缺少 tags、rating、createdAt、updatedAt，与 Backend 的 12 字段定义不统一。此外，episodeCount 约束也不同：Web 使用 `positive()`（>0），Backend 使用 `nonnegative()`（>=0）。由于 design.md 不存在，没有权威定义来裁决应以哪一端为准。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Web-2：HealthResponseSchema 与 Backend 端不一致

- **严重程度**：🟡 关注
- **平台**：web
- **维度**：数据模型一致性
- **描述**：与 Backend-2 对应。Web 的 HealthResponseSchema 使用 `z.literal('ok')`，未包含 Backend 即将新增的 database 和 redis 字段。如果 Web 用此 Schema 校验 Backend 返回的扩展后的响应，`database`/`redis` 字段将被 Zod 丢弃（非 strict 模式下）或校验失败，导致数据不完整。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Web-3：metadata 中硬编码产品名称

- **严重程度**：🟡 关注
- **平台**：web
- **维度**：平台规范
- **描述**：Section 7.4 的 RootLayout metadata 中直接写了 `'ShortDrama'` 和 `'ShortDrama content platform'`。根据 CLAUDE.md「禁止硬编码常量」的要求，以及 Web 端已有 `lib/config.ts` 管理应用名称，建议改为引用 config：
  ```typescript
  import { config } from '@/lib/config';
  // ...
  title: {
    default: config.app.name,
    template: `%s — ${config.app.name}`,
  },
  ```
  此外，CLAUDE.md 规定「skill、subagent、CLAUDE.md 等元内容文件不得内嵌任何具体的产品信息，应改为引用 PRODUCT.md」，此约束虽针对元内容文件，但遵循同样的解耦原则：设计文档中的示例代码不应硬编码产品名。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Web-4：zod 依赖状态不明确

- **严重程度**：🟡 关注
- **平台**：web
- **维度**：平台规范
- **描述**：现有 Web 工程 `src/lib/schemas.ts` 已经 `import { z } from 'zod'` 并定义了 DramaSchema，但 Section 14「新增依赖」中将 zod 列为「需在本轮实现时安装」。这有两种可能：(a) schemas.ts 引用了尚未安装的依赖，代码不可运行；(b) zod 已安装但文档未更新。需要明确实际状态。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Web-5：请求重试策略仅描述但未在 api-client 代码中实现

- **严重程度**：🟡 关注
- **平台**：web
- **维度**：边界与错误处理
- **描述**：Section 6.3 详细描述了请求重试策略（指数退避、2 次重试、5xx 自动重试），但 Section 6.2 的 `apiFetch` 实现中并没有对应的重试逻辑。当前实现仅在超时/网络错误时抛出对应异常，没有重试机制。建议明确标注此功能为「预留设计，后续实现」，或将该段落移到「后续演进」章节。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Web-6：api-client 中的硬编码 Content-Type

- **严重程度**：🟢 建议
- **平台**：web
- **维度**：平台规范
- **描述**：Section 6.2 的 api-client 中 `'Content-Type': 'application/json'` 是硬编码字符串。虽然这是标准值，但根据 CLAUDE.md 规范，建议从常量文件统一管理或至少添加注释说明。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Web-7：缺少 not-found 路由对无效 ID 格式的处理

- **严重程度**：🟢 建议
- **平台**：web
- **维度**：边界与错误处理
- **描述**：Section 5.1 定义了 `/play/[id]` 和 `/detail/[id]` 路由，id 参数为 `string` 类型。但方案未说明当 id 格式不合法（如空字符串、非 UUID 格式）时，PlayPage/DetailPage 应如何处理。建议在方案中明确：是在 Page 组件中做格式校验并调用 `notFound()`，还是仅在后续对接 API 时由 API 返回 404。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Web-8：hydration mismatch 风险仅列出但无具体对策

- **严重程度**：🟢 建议
- **平台**：web
- **维度**：边界与错误处理
- **描述**：Section 12.4 列出了 hydration mismatch 风险，对策为「严格保证 SSR 输出确定性」。但方案中未给出具体的保障措施，例如：如何确保 config 读取不产生服务端/客户端差异、如何避免 `Date.now()` 或 `Math.random()` 等非确定性值在 SSR 中使用。建议补充至少一条具体对策。
- **修复状态**：❌ 未修复
- **修复说明**：—

#### 问题 Web-9：Container 组件 css 文件使用 .module.css 但其他组件也使用 .module.css — 风格一致性

- **严重程度**：🟢 建议
- **平台**：web
- **维度**：架构合理性
- **描述**：Section 2.1 定义了五层架构，其中 Design System 层放在 `styles/` 下（globals.css + tokens.css），但 Shared UI 组件的样式使用 CSS Modules（`.module.css`）。建议明确说明这些设计决策：哪些样式走全局 CSS 变量，哪些走 CSS Modules，避免未来风格分裂。
- **修复状态**：❌ 未修复
- **修复说明**：—

## 跨端一致性检查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| API 调用与 Shared 设计一致 | ❌ 无法检查 | design.md 不存在，无法验证 |
| 数据模型各端一致 | ❌ 不一致 | DramaSchema 字段数量不同（Backend 12 vs Web 6），episodeCount 约束不同（positive vs nonnegative）；HealthResponseSchema 字段数量不同（Backend 5 vs Web 3） |
| 共享逻辑覆盖 | ⚠️ 部分 | Web 的 schema 声明已与 Backend 不一致，共享逻辑覆盖不完整 |
| 错误处理策略一致 | ⚠️ 部分 | Backend 有统一错误码体系，Web 有错误码映射表，但仅在 Web 端定义了网络超时等前端特有错误码，Backend 未定义对应的 HTTP 状态码映射 |

## 遗留问题（需人工决策）

| 编号 | 问题 | 平台 | 建议 | 状态 |
|------|------|------|------|------|
| H-01 | spec.md 和 design.md 是否应在本轮补齐，还是后续单独开一轮？当前仅有 design-backend.md 和 design-web.md，审查可执行的范围受限。 | all | 建议先补齐 spec.md 和 design.md，再进行完整审查；或明确本轮仅审查已产出的两个平台方案，缺失文档作为已知 gap 记录 | 待确认 |
| H-02 | iOS 和 Android 设计是否需要在当前阶段产出？两端工程已初始化但无方案文档。 | ios / android | 如果 spec 中涉及 iOS/Android 需求，建议本轮补齐；如果本轮仅初始化后端和 Web，需在 spec 中明确 | 待确认 |
| H-03 | DramaSchema 以哪一端为准？Backend 12 字段 vs Web 6 字段。 | backend / web | 建议在 design.md 中定义权威数据模型，两端对齐 | 待确认 |
| H-04 | RESTful 合规 vs 功能语义：`/api/player/start` 和 `/api/player/stop` 作为 RPC 风格端点是否可接受？ | backend | 建议改为 RESTful 风格（如 `/api/play-sessions`），但如果团队认为播放控制语义更适合动词路径，需要在 design.md 中给出明确的 ADR | 待确认 |
| H-05 | config.ts 中的硬编码回退值是否可接受？现有实现使用 `?? 'postgresql://...'` 作为开发默认值。 | backend | 建议移除所有回退值，强制通过环境变量注入；如果保留，需要团队明确同意并在 design.md 的 ADR 中记录 | 待确认 |
| H-06 | 新增开源依赖（pg, ioredis, drizzle-orm, @supabase/supabase-js, vitest, @testing-library/react 等）是否已获得用户批准？ | backend / web | 根据根目录 CLAUDE.md「新增开源依赖前必须征得用户同意」，建议在推进实施前确认 | 待确认 |

## 结论

- [x] ✅ 关键问题已修复
  - G-1/G-2/G-3：误报（文件路径问题），所有文件实际存在
  - Backend-1：DramaSchema 字段统一为 design.md 权威定义，episodeCount 约束统一为 `positive()`
  - Backend-2：HealthResponseSchema 扩展已在 design.md 中定义，Backend 端对齐
  - Backend-5：Supabase demo keys 已替换为占位符
  - Backend-7：错误码定义已扩展，新增 UNAUTHORIZED/FORBIDDEN/CONFLICT/TOO_MANY_REQUESTS
  - H-06：新增依赖（@supabase/supabase-js、ioredis、vitest）已获用户批准
- [x] ✅ Backend 设计已重构为 Supabase 基础服务架构
- [ ] ⚠️ 待 design-human-review 确认
- [ ] 🔴 用户指示不推进到下一阶段（停留在 design-human-review）

---

### 问题统计

| 严重程度 | 数量 | 说明 |
|---------|------|------|
| 🔴 阻塞 | 6 | G-1(spec缺失), G-2(design缺失), G-3(iOS/Android方案缺失), Backend-1(DramaSchema不一致), Web-1(DramaSchema不一致), 跨端数据模型不一致 |
| 🟡 关注 | 11 | Backend-2~10, Web-2~5 |
| 🟢 建议 | 6 | Backend-11~12, Web-6~9 |
| ⚠️ 待确认 | 6 | H-01~H-06 |
