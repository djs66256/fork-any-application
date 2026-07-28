# PRD-12 剧场频道 wiki 收录报告

> 生成时间：2026-07-28
> 依据：已合并到 `master` 的 PRD-12 剧场频道实际代码变更

## 收录范围

本次收录基于以下来源完成：

- 需求 / 设计 / 计划 / QA 输入：
  - `docs/specs/2026-07-28-prd-12-theater-channel/spec.md`
  - `docs/specs/2026-07-28-prd-12-theater-channel/spec-review.md`
  - `docs/specs/2026-07-28-prd-12-theater-channel/design.md`
  - `docs/specs/2026-07-28-prd-12-theater-channel/design-review.md`
  - `docs/specs/2026-07-28-prd-12-theater-channel/design-backend.md`
  - `docs/specs/2026-07-28-prd-12-theater-channel/design-ios.md`
  - `docs/specs/2026-07-28-prd-12-theater-channel/design-android.md`
  - `docs/specs/2026-07-28-prd-12-theater-channel/plan-backend.md`
  - `docs/specs/2026-07-28-prd-12-theater-channel/plan-ios.md`
  - `docs/specs/2026-07-28-prd-12-theater-channel/plan-android.md`
  - `docs/specs/2026-07-28-prd-12-theater-channel/qa-test.md`
- 代码事实来源：Android / iOS / Backend 已合并代码（以 `master` 上 PRD-12 实现为准）

## 更新 / 新增的 wiki 文件

### 新增

- `wiki/features/theater/index.md`
- `wiki/revision/2026-07-28-prd-12-theater-channel.md`

### 更新

- `wiki/features/index.md`
- `wiki/features/app-shell/index.md`
- `wiki/features/search-discovery/index.md`
- `wiki/features/classification/index.md`
- `wiki/features/ranking/index.md`
- `wiki/features/video-player/index.md`
- `wiki/api/index.md`
- `wiki/api/dramas.md`
- `wiki/architecture/overview.md`

## 本次收录的关键结论

1. 剧场已不是占位 tab，而是 Android / iOS 上真实落地的一级频道。
2. Backend 已实现 `GET /api/dramas/channel`，并形成可消费契约。
3. 剧场默认请求为 `channel=all&page=1&pageSize=20`。
4. 当前只有 `all` 频道返回真实内容，其余 `real / anime / movie / audio / novel / comic / bigscreen` 都是合法空态，而不是错误页。
5. Backend 返回原始整数 `heat`，格式化责任在客户端：
   - Android 客户端格式化为“万”单位。
   - iOS 客户端格式化为“万 / 亿”单位。
6. 剧场快捷入口不会在剧场 tab 内重复实现搜索/分类/排行/新剧页面，而是切回首页所属导航栈复用既有页面。
7. 预约快捷入口会直接进入排行页的预约榜上下文：
   - Android 通过 `ranking?contentType=all&type=booking`
   - iOS 通过 `TheaterRankingEntryContext(rankingType: .booking)`
8. 剧场卡片点击继续复用 canonical `play` 主路径，不创建剧场专属播放器语义。

## wiki.md 是否已生成

已生成：`docs/specs/2026-07-28-prd-12-theater-channel/wiki.md`

## 是否存在无法确认、需要人工决策的问题

无。

当前代码事实足以支持本次 wiki 收录，未发现必须等待人工判定后才能落档的歧义点。

## 验证步骤与结果

### 1. Mermaid 语法验证

- 验证范围：本次变更的 12 个 wiki / report Markdown 文件
- 结果：全部通过
- 说明：逐文件审计后，12 个文件中 Mermaid 代码块数量均为 `0`，因此无需额外执行 Mermaid 语法解析

### 2. 交叉引用 / 文件引用验证

- 验证方式：逐文件核对内部链接目标与主要代码文件路径
- 结果：通过，未发现失效引用
- 明细：
  - `wiki/features/index.md`：11 / 11 有效
  - `wiki/features/theater/index.md`：135 / 135 有效
  - `wiki/features/app-shell/index.md`：117 / 117 有效
  - `wiki/features/search-discovery/index.md`：116 / 116 有效
  - `wiki/features/classification/index.md`：102 / 102 有效
  - `wiki/features/ranking/index.md`：104 / 104 有效
  - `wiki/features/video-player/index.md`：99 / 99 有效
  - `wiki/api/index.md`：5 / 5 有效
  - `wiki/api/dramas.md`：7 / 7 有效
  - `wiki/architecture/overview.md`：0 / 0 有效
  - `wiki/revision/2026-07-28-prd-12-theater-channel.md`：61 / 61 有效
  - `docs/specs/2026-07-28-prd-12-theater-channel/wiki.md`：38 / 38 有效

## 收录后建议

- 后续若非 `all` 子频道开始接入真实内容，应优先更新：
  - `wiki/features/theater/index.md`
  - `wiki/api/dramas.md`
  - `wiki/architecture/overview.md`
- 若 Web 后续补齐剧场页承载，再补充 Web 范围变更，不建议提前在 wiki 中假设 Web 结构
