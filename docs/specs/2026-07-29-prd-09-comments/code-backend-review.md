# 代码 Review：Backend — PRD-09 评论系统

> Review 日期：2026-07-29

## 审查结果

| 维度 | 状态 | 说明 |
|------|------|------|
| 实现与 design 一致性 | ✅ | 已按 design-backend 中的 Route → Service → Repository → Shared 分层补齐评论列表、发表评论、点赞切换、migration 与 repository registry。 |
| 无硬编码常量 | ✅ | 未新增环境地址、token、固定业务环境常量；仓储切换走 `config.comments.repository`。 |
| 代码风格符合平台规范 | ✅ | 新增代码遵循 TypeScript + Zod + AppError 约定；lint 无 error。 |
| 错误处理完备 | ✅ | 覆盖 `DRAMA_NOT_FOUND`、`COMMENT_NOT_FOUND`、`UNAUTHORIZED`、`VALIDATION_ERROR`、`SERVICE_UNAVAILABLE`、`INTERNAL_ERROR`。 |
| 性能无明显问题 | ✅ | 评论列表分页、排序在 mock/supabase 两套仓储中都按设计执行；新增索引覆盖 `drama_id + created_at` 与 `drama_id + like_count + created_at`。 |
| API 调用一致性 | ✅ | route 成功响应分别保持 `{ data, pagination }`、`Comment`、`{ comment_id, liked, like_count }` canonical contract。 |
| 所有测试通过 | ✅ | 定向测试、全量测试、build 已通过；lint 无 error。 |
| 分层职责清晰 | ✅ | route 仅做入参解析/认证/service 调用；service 统一做 drama 校验与输出 parse；repository 负责数据读写。 |
| 数据契约可测试 | ✅ | schemas、mock repository、supabase repository、service、route 均有自动化测试覆盖。 |

## 变更文件审查

| 文件 | 审查结果 | 问题数 |
|------|---------|--------|
| `backend/src/lib/schemas.ts` | ✅ | 0 |
| `backend/src/lib/errors.ts` | ✅ | 0 |
| `backend/src/lib/config.ts` | ✅ | 0 |
| `backend/src/repositories/interfaces/comment.repository.interface.ts` | ✅ | 0 |
| `backend/src/repositories/repository-registry.ts` | ✅ | 0 |
| `backend/src/repositories/mock/comment.mock.repository.ts` | ✅ | 0 |
| `backend/src/repositories/supabase/comment.supabase.repository.ts` | ✅ | 0 |
| `backend/src/services/comment/comment.service.ts` | ✅ | 0 |
| `backend/src/app/api/dramas/[id]/comments/route.ts` | ✅ | 0 |
| `backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts` | ✅ | 0 |
| `backend/src/lib/__tests__/schemas.test.ts` | ✅ | 0 |
| `backend/src/lib/__tests__/config.test.ts` | ✅ | 0 |
| `backend/src/repositories/__tests__/comment.mock.repository.test.ts` | ✅ | 0 |
| `backend/src/repositories/supabase/__tests__/comment.supabase.repository.test.ts` | ✅ | 0 |
| `backend/src/services/comment/comment.service.test.ts` | ✅ | 0 |
| `backend/src/app/api/__tests__/dramas-comments.test.ts` | ✅ | 0 |
| `backend/supabase/migrations/20260729000100_add_comments_tables.sql` | ✅ | 0 |

## 发现的问题

### 问题 1：本地 Supabase migration 验证受既有环境阻塞

- **严重程度**：🟡 中
- **文件**：`backend/supabase/migrations/20260729000100_add_comments_tables.sql`
- **类型**：验证-未修复
- **描述**：已执行 `docker compose -f tests/docker-compose.yml up -d` 成功拉起本地依赖，但 `npx supabase db push` 在当前环境下先因未 link 项目失败；改用 `--db-url` 直连后，又被既有历史 migration `20260727000200_add_role_to_profiles.sql` 中 `CREATE TYPE user_role AS ENUM ...` 阻塞，导致无法继续验证到本次 comments migration。
- **建议修复**：后续需统一本地 Supabase CLI 使用方式，并处理历史 migration 在重复执行场景下的幂等性，再重新执行 `db push` 验证 comments migration。
- **修复状态**：❌ 未修复
- **修复方案**：本次如实记录环境限制，不修改历史 migration，避免引入超出 PRD-09 backend 范围的副作用。

## 修复记录

| 轮次 | 修复项 |
|------|--------|
| 1 | 新增 comments route 测试并修正热度排序断言；修正 mock repository 测试中对最新创建评论排序的错误假设；调整 supabase repository 测试 mock 链式调用；将 Supabase repository 的脏行解析失败统一包装为 `INTERNAL_ERROR`。 |

## 遗留问题（需人工决策）

| 编号 | 问题 | 文件 | 建议 | 状态 |
|------|------|------|------|------|
| H-01 | 本地 Supabase migration 验证被既有历史 migration 幂等性问题阻塞 | `backend/supabase/migrations/20260727000200_add_role_to_profiles.sql` | 单独修复历史 migration 的本地重复执行兼容性后，再回归执行 `npx supabase db push` | 待确认 |

## 结论

- [ ] ✅ 所有问题已修复，代码质量合格
- [x] ⚠️ 存在遗留问题，需人工确认
