# 代码 Review：Backend — PRD-07 菜单面板

> Review 日期：2026-07-28

## 审查结果

| 维度 | 状态 | 说明 |
|------|------|------|
| 实现与 design 一致性 | ✅ | 新增 `GET /api/player/recently-viewed`，分层保持 Route → Service → Repository → Shared，返回上限 3 条，允许过滤后不足 3 条，与 spec/design/design-backend 一致 |
| 无硬编码常量 | ✅ | 业务上限与候选窗口已收敛到 `backend/src/lib/player.ts`，未引入环境地址、token、固定环境值等硬编码 |
| 代码风格符合平台规范 | ✅ | TypeScript + Zod + Next.js Route Handler 用法与现有 backend 风格一致；抽取了共享 header 解析 helper 以减少重复 |
| 错误处理完备 | ✅ | 缺失/非法 header 统一映射 `INVALID_PLAYBACK_SESSION`；服务端响应映射失败统一转 `INTERNAL_ERROR`；Supabase 可用性异常映射 `SERVICE_UNAVAILABLE` |
| 性能无明显问题 | ✅ | 使用固定候选窗口查询最近记录，按 `updated_at desc` 排序并在聚合到 3 条后提前停止；未实现 offset 回填，符合已收敛策略 |
| API 调用一致性 | ✅ | 继续复用 `X-Playback-Session-Id`、统一 JSON envelope、`withErrorHandler`、repository-registry 注入方式 |
| 所有测试通过 | ✅ | `npm run test`、`npm run build` 均通过；`npm run lint` 无 error，仅存在仓库既有 5 条 warning，与本次改动无关 |
| RESTful / 响应契约合规 | ✅ | Route 为只读 GET，参数来源于 header，请求/响应结构遵循既有 player 域契约 |
| 数据访问与脏数据过滤质量 | ✅ | mock / Supabase repository 均支持按 session 最近记录查询；service 过滤缺失 drama、缺失 episode、跨 drama episode 的脏数据 |

## 变更文件审查

| 文件 | 审查结果 | 问题数 |
|------|---------|--------|
| `backend/src/lib/player.ts` | ✅ | 0 |
| `backend/src/lib/schemas.ts` | ✅ | 0 |
| `backend/src/lib/__tests__/schemas.test.ts` | ✅ | 0 |
| `backend/src/repositories/interfaces/playback-history.repository.interface.ts` | ✅ | 0 |
| `backend/src/repositories/mock/playback-history.mock.repository.ts` | ✅ | 0 |
| `backend/src/repositories/__tests__/playback-history.mock.repository.test.ts` | ✅ | 0 |
| `backend/src/repositories/supabase/playback-history.supabase.repository.ts` | ✅ | 0 |
| `backend/src/repositories/supabase/__tests__/playback-history.supabase.repository.test.ts` | ✅ | 0 |
| `backend/src/services/player/player.service.ts` | ✅ | 0 |
| `backend/src/services/player/player.service.test.ts` | ✅ | 0 |
| `backend/src/app/api/player/parse-playback-session-id.ts` | ✅ | 0 |
| `backend/src/app/api/player/progress/route.ts` | ✅ | 0 |
| `backend/src/app/api/player/start/route.ts` | ✅ | 0 |
| `backend/src/app/api/player/stop/route.ts` | ✅ | 0 |
| `backend/src/app/api/player/recently-viewed/route.ts` | ✅ | 0 |
| `backend/src/app/api/__tests__/player.recently-viewed.test.ts` | ✅ | 0 |

## 发现的问题

无新增遗留问题。本轮 review 中识别出的实现细化项均已修复并完成回归验证。

### 问题 1：recently-viewed 时间字段 contract 初版过宽

- **严重程度**：🟡 中
- **文件**：`backend/src/lib/schemas.ts:283`
- **类型**：规范违反
- **描述**：`updated_at` 初版仅限制为字符串，未与 design 要求的 ISO datetime 保持一致。
- **建议修复**：使用 `z.string().datetime()` 收紧时间字段格式。
- **修复状态**：✅ 已修复
- **修复方案**: 已将 `RecentlyViewedItemSchema.updated_at` 调整为 `z.string().datetime()`，并补充失败路径验证测试。

### 问题 2：recently-viewed 上限常量初版存在重复定义

- **严重程度**：🟢 低
- **文件**：`backend/src/lib/schemas.ts:291`、`backend/src/services/player/player.service.ts:126`
- **类型**：可维护性
- **描述**：返回上限 3 的业务常量在 schema 与 service 两处散落，后续易产生漂移。
- **建议修复**：抽取共享常量，统一 schema 与 service 引用。
- **修复状态**：✅ 已修复
- **修复方案**: 新增 `backend/src/lib/player.ts`，集中声明 `RECENTLY_VIEWED_LIMIT` 与 `RECENTLY_VIEWED_FETCH_LIMIT`。

### 问题 3：player 路由 header 解析存在重复实现

- **严重程度**：🟢 低
- **文件**：`backend/src/app/api/player/progress/route.ts`、`backend/src/app/api/player/start/route.ts`、`backend/src/app/api/player/stop/route.ts`、`backend/src/app/api/player/recently-viewed/route.ts`
- **类型**：可维护性
- **描述**：`X-Playback-Session-Id` 校验逻辑在多个 route 内重复，存在维护分叉风险。
- **建议修复**：抽取共享 helper，统一 player 域路由使用。
- **修复状态**：✅ 已修复
- **修复方案**: 新增 `backend/src/app/api/player/parse-playback-session-id.ts`，四个 player route 已统一复用。

### 问题 4：recently-viewed 响应映射失败初版可能误报为 400

- **严重程度**：🟡 中
- **文件**：`backend/src/services/player/player.service.ts:131`
- **类型**：错误处理
- **描述**：若使用 `RecentlyViewedResponseSchema.parse(...)` 直接抛出 ZodError，middleware 可能将服务端映射失败包装成客户端 400 校验错误。
- **建议修复**：使用 `safeParse` 后显式抛出 `Errors.internal(...)`。
- **修复状态**：✅ 已修复
- **修复方案**: 已改为 `safeParse`，失败时统一抛出 `INTERNAL_ERROR`，并补充 service / route 失败路径测试。

### 问题 5：Supabase 最近记录查询初版未区分可用性异常

- **严重程度**：🟡 中
- **文件**：`backend/src/repositories/supabase/playback-history.supabase.repository.ts:50`
- **类型**：错误处理
- **描述**：Supabase 网络/连接/超时类异常若全部映射为 `INTERNAL_ERROR`，不利于上层区分基础设施不可用场景。
- **建议修复**：对典型连接失败与 SQLSTATE 可用性错误码映射 `SERVICE_UNAVAILABLE`。
- **修复状态**：✅ 已修复
- **修复方案**: 已对 `failed to fetch`、`network`、`timeout`、`connection` 及 `08000/08003/08006/57p01` 等场景映射 `Errors.serviceUnavailable('playback_history')`，并补充 repository 测试。

## 修复记录

| 轮次 | 修复项 |
|------|--------|
| 1 | 实现 recently-viewed schema / repository / service / route 主链路，并补齐对应自动化测试 |
| 2 | 根据 review 收紧 `updated_at` 为 ISO datetime；抽取共享 limit 常量；抽取共享 header 解析 helper；修正 recently-viewed 映射失败的错误归类；补充 Supabase 可用性错误映射与失败路径测试 |
| 3 | 回归执行 `player.progress` / `player.start` / `player.stop` 路由测试、全量 `npm run test`、`npm run build`、`npm run lint`，确认无回归 |

## 上一轮问题修复验证

| 问题编号 | 原问题摘要 | 原修复状态 | 验证结果 | 说明 |
|---------|-----------|-----------|---------|------|
| #1 | `updated_at` contract 过宽 | ✅ 已修复 | ✅ 已验证修复 | schema 已限制为 ISO datetime，相关失败路径已有测试覆盖 |
| #2 | recently-viewed 上限常量重复定义 | ✅ 已修复 | ✅ 已验证修复 | schema / service 已统一依赖 `backend/src/lib/player.ts` |
| #3 | player 路由 header 解析重复 | ✅ 已修复 | ✅ 已验证修复 | 四个 player route 已统一复用共享 helper，相关旧路由测试通过 |
| #4 | 响应映射失败可能误报 400 | ✅ 已修复 | ✅ 已验证修复 | route 与 service 失败路径测试均断言 `INTERNAL_ERROR` |
| #5 | Supabase 可用性异常未区分 | ✅ 已修复 | ✅ 已验证修复 | repository 测试已覆盖 `SERVICE_UNAVAILABLE` 分支 |

## 遗留问题（需人工决策）

无。

## 结论

- [x] ✅ 所有问题已修复，代码质量合格
- [ ] ⚠️ 存在遗留问题，需人工确认
