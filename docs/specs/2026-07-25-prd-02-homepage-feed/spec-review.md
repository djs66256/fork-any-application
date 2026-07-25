# 需求 Review：PRD-02 首页信息流

> Review 日期：2026-07-25
> Review 循环：第 2 轮
> 审查者：AI Agent

## 审查结果总览

| 维度 | 结论 | 说明 |
|------|------|------|
| 完整性 | ✅ 通过 | 上一轮提出的 4 个问题已收口：canonical contract、MVP 默认决策、分页前台语义、搜索/榜单导流范围均已写清 |
| 一致性（与 PRODUCT / 代码） | ⚠️ 部分通过 | 页面承载策略、Backend 路径/分页 query、Android/iOS 现状描述已基本对齐，但仍有 1 处与代码事实不一致 |
| 可实现性 | ⚠️ 有前置修正项 | 需求本身已可推进，但进入设计前需先修正文档中的响应示例，避免误导实现 |
| 平台覆盖 | ✅ 通过 | Backend / iOS / Android 纳入，Web 明确不做 Feed，与当前产品策略一致 |
| 页面承载策略 | ✅ 通过 | `PRODUCT.md` 中 mall / earn 为 H5，其它业务页默认 Native 的约束已正确落实到 spec |
| 与当前接口/客户端事实一致性 | ⚠️ 部分通过 | 已正确记录 Backend `/api/dramas`、Android `page_size` 现状、iOS `/api/v1/dramas` + 包裹响应现状，但接口示例中的 `id` 样例仍偏离 Backend 当前 schema |

## 结论

- **总体结论**：当前 `spec.md` 已解决上一轮 review 的 4 个问题，MVP 范围、跨端收口方向与页面承载策略已经足够清晰。
- **推进建议**：在修正下述唯一遗留问题后，**建议进入 spec-human-review**；若团队接受该问题作为文档级小修，也可在 human review 前顺手修正，不构成重新做需求收敛的阻塞。

---

## 发现的问题

### 问题 1：Canonical API 示例中的 `id` 样例与 Backend 当前 schema 的 UUID 事实不一致

- **严重程度**：🟡 中
- **维度**：一致性（与代码）
- **描述**：
  Section 6.1 的 canonical API 示例使用了 `"id": "drama-001"`（`spec.md:428`），但 Backend 当前 `DramaSchema` 明确要求 `id` 为 `uuid()`（`backend/src/lib/schemas.ts:15-18`），Route / Repository / 测试链路也均围绕 UUID 约束组织。当前示例会给 design/coding 传递出“首页卡片 id 可以是业务短串”的错误信号。
- **影响**：
  如果设计稿、接口 mock 或客户端测试用例照着 `drama-001` 这类样例继续扩散，后续实现阶段容易出现：
  1. 客户端 mock 与 Backend schema 不一致；
  2. 播放/详情路由参数示例与真实接口数据格式脱节；
  3. QA 在构造验收数据时误以为接口不要求 UUID。
- **可执行修复建议**：
  1. 将示例中的 `id` 改为 UUID 形态（如 `550e8400-e29b-41d4-a716-446655440000`）；
  2. 如需表达“展示层可使用业务文案编号”，请与 API 主键分开描述，不要混入 canonical response 示例。

---

## 正向结论（本轮通过项）

1. **上一轮 4 个问题已全部解决，不应重复作为本轮问题保留**
   - 跨端 API 契约已在 Section 6.1/6.3 与 Section 7 中收敛到 canonical contract；
   - 2 个阻塞性产品决策已在 Section 5.2 与 Section 10 中落定；
   - 分页前台语义已明确为“客户端本期只消费首页首屏第一页，分页主要作为接口契约与后续扩展预留”；
   - 搜索/榜单导流已明确排除在本期范围外，仅保留内容卡片主路径。

2. **与 `PRODUCT.md` 页面承载策略一致**
   - `PRODUCT.md` 明确：mall / earn 走 H5，其它业务页默认 Native（`PRODUCT.md:22-25`）；
   - `spec.md` 已正确限定：本期仅推进 Native 首页 Feed，不扩展到商城/赚钱业务页与 Web Feed（`spec.md:31-39`）。

3. **与 Backend 当前 `/api/dramas` 事实基本一致**
   - Backend Route 当前确实是 `GET /api/dramas`，query 使用 `page` + `pageSize`（`backend/src/app/api/dramas/route.ts:8-24`）；
   - 返回外层结构确实是 `{ data, pagination }`，且 `pagination` 为 snake_case（`backend/src/repositories/interfaces/drama.repository.interface.ts:8-16`、`backend/src/app/api/__tests__/dramas.test.ts:12-28`）；
   - Spec 已准确记录当前 schema 仍是 `total_episodes`、缺少 `tags`，并把这件事定义为本期收口项（`spec.md:461-463`）。

4. **与 Android 当前 query / DTO 现状一致**
   - Android 当前仍请求 `page_size`（`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:20-24`）；
   - DTO 当前消费 `episode_count`、`tags`、snake_case pagination（`android/app/src/main/java/com/djs66256/short_drama/data/dto/DramaDto.kt:7-23`、`android/app/src/main/java/com/djs66256/short_drama/data/dto/PaginationDto.kt:6-14`）；
   - Spec 已将其正确描述为“现状”而非误写成“已完成收口”。

5. **与 iOS 当前 `/api/v1/dramas` + 响应包裹现状一致**
   - iOS 当前数据源仍请求 `/api/v1/dramas?page&page_size`，并期望 `{ code, data: { items, pagination } }`（`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift:3-35,45-70`）；
   - Spec 已正确把这部分写成待迁移现状，而不是错误宣称已与 Backend 一致（`spec.md:463`）。

6. **MVP 范围和验收口径已可支撑后续 design**
   - 常规列表卡片、`drama.id -> videoId` 占位映射、仅消费第一页、无搜索/榜单导流入口，这几项关键决策都已固定；
   - 对 design / plan 阶段而言，当前 spec 已具备足够稳定的范围边界与接口目标。

---

## 建议的处理顺序

1. **先修正文档示例中的 `id` 格式**（低成本、避免误导）
2. **随后进入 spec-human-review**
3. human review 通过后进入 shared design / 平台 design 拆分

## 最终判断

- [x] ✅ **修正上述文档级问题后，建议进入 spec-human-review**
- [ ] ⚠️ 仍有阻塞性问题，不建议推进

## 问题数量统计

| 严重程度 | 数量 |
|---------|------|
| 🔴 严重 | 0 |
| 🟠 高 | 0 |
| 🟡 中 | 1 |
| 🟢 低 | 0 |
| **合计** | **1** |
