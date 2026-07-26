# 需求 Review：PRD-03 完整观看播放器

> Review 日期：2026-07-26
> Review 循环：第 3 轮
> 审查者：AI Agent

## 审查结果总览

| 维度 | 结论 | 说明 |
|------|------|------|
| 完整性 | ✅ 通过 | 主路径、倍速、选集、断点续播、错误态、互动栏与状态恢复均已覆盖 |
| 边界与错误处理 | ✅ 通过 | 已覆盖加载失败、无资源、恢复点异常、切集、切后台恢复等核心边界 |
| 一致性（与代码 / wiki / PRD） | ✅ 通过 | 当前播放器占位、Backend 501、Native 承载、`play/:id` 路由兼容等事实已对齐 |
| 可行性 | ✅ 通过 | 启播链路收口为 progress → episodes → start，客户端与 Backend 可按此拆分实现 |
| 平台覆盖 | ✅ 通过 | Backend / iOS / Android 均已纳入，Web 明确不在本期范围 |
| 术语与范围 | ✅ 通过 | `videoId` 仅保留为兼容命名，内部语义已统一解释为 `drama` 播放目标 |

## 本轮已收口问题

### 问题 1：匿名续播身份缺少明确接口传输契约

- **严重程度**：🟡 中
- **维度**：可行性
- **描述**：首版未登录续播虽已确定采用匿名会话标识，但此前未写清生成方式、持久化方式，以及哪些接口必须携带该身份。
- **修复状态**：✅ 已修复
- **修复说明**：已在 spec 中明确：客户端首次启动生成 UUID 并持久化；首版仅 `GET /api/player/progress`、`POST /api/player/start`、`POST /api/player/stop` 三个接口必须通过 `X-Playback-Session-Id` header 透传；`GET /api/dramas/:id/episodes` 首版不要求该 header。

### 问题 2：spec 与 copied PRD / subtasks 存在续播契约漂移

- **严重程度**：🟡 中
- **维度**：一致性
- **描述**：此前 supporting docs 仍残留旧口径，如 `POST /api/player/start` 返回 `start_time`、`GET /api/player/progress` 依赖 `episodeId`、播放历史依赖 `user_id` 等，和最新版 spec 冲突。
- **修复状态**：✅ 已修复
- **修复说明**：已同步 `docs/product_manager/prd/2026-07-25-full-player/prd.md` 与 `subtasks.md`，统一为：
  1. canonical 启播查询为 `GET /api/player/progress?dramaId=...`；
  2. `POST /api/player/start` 仅在客户端已明确 `episode_id` 后调用；
  3. 首版匿名续播通过 `playback_session_id` / `X-Playback-Session-Id` 归属；
  4. 切集固定从第 0 秒开始，不额外查询历史恢复点。

## 修改记录

| 轮次 | 修改项 | 修改内容 |
|------|--------|---------|
| 1 | 主链路收口 | 将播放器初始化链路收口为 `GET /api/player/progress` → `GET /api/dramas/:id/episodes` → `POST /api/player/start` |
| 2 | 续播契约补齐 | 增补匿名续播 UUID、`X-Playback-Session-Id` header、默认集 / 恢复集回退规则 |
| 3 | supporting docs 对齐 | 同步 PRD / subtasks 的 API 契约、Sprint 拆分、切集与续播规则 |

## 结论

- [x] ✅ 所有问题已修复，可进入下一阶段（spec-human-review）
- [ ] ⚠️ 存在遗留问题，需要人工确认后再推进
