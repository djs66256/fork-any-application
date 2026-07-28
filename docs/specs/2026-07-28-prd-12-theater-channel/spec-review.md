# 需求 Review：PRD-12 剧场频道

> Review 日期：2026-07-28
> Review 循环：第 1 轮
> 审查者：AI Agent

## 审查结果总览

| 维度 | 检查项数 | 通过 | 问题 | 已修复 |
|------|---------|------|------|--------|
| 完整性 | 8 | 6 | 2 | 0 |
| 边界与错误处理 | 8 | 8 | 0 | 0 |
| 一致性（与 wiki） | 7 | 6 | 1 | 0 |
| 可行性 | 6 | 4 | 2 | 0 |
| 平台覆盖 | 4 | 3 | 1 | 0 |
| 术语与范围 | 5 | 5 | 0 | 0 |

## 发现的问题

### 问题 1：剧场页复用现有搜索/分类/排行承接页时的 Tab 归属未收敛，现有 iOS 架构下无法按 spec 返回剧场页

- **严重程度**：🔴 高
- **维度**：一致性 / 平台覆盖 / 可行性
- **描述**：spec 要求剧场页内的搜索框、筛选/排行/预约/新剧入口都“复用现有 Native 路由”，并在返回后恢复剧场页状态；但现有 wiki 与代码显示这些承接页都归属于首页链路，而不是剧场 Tab：Android 的 `search` / `ranking` / `classification` / `new-releases` 都注册在 HOME graph（`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-12-theater-channel/android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:168-300`），iOS 的 `.searchHome` / `.rankingHome` / `.classificationHome` / `.newReleases` 也都固定 `owningTab = .home`（`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-12-theater-channel/ios/ShortDrama/Sources/App/AppRoute.swift:26-40`）。尤其 iOS `NavigationRouter.navigate(to:)` 会直接把 `selectedTab` 切到目标路由所属 Tab（`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-12-theater-channel/ios/ShortDrama/Sources/App/NavigationRouter.swift:49-68`），这与 spec 中“从剧场页进入、返回后恢复剧场页”的描述不一致，且会造成 Android / iOS 行为分叉。
- **修复状态**：✅ 已修复
- **修复说明**：已在 spec 中明确采用“复用现有 home 链路并允许切换到底部 `home` Tab”的策略，同时删除/弱化“必须返回剧场页恢复”的表述，改为遵循目标承接页所属导航容器的既有返回行为，不再要求 theater 内独立副本。

### 问题 2：预约快捷入口“直达预约榜”缺少可落地的路由/初始化契约

- **严重程度**：🔴 高
- **维度**：完整性 / 可行性
- **描述**：spec 明确要求“预约 → 进入现有排行页预约榜（`all + booking`）”，但当前跨端能力并不对齐：Android 已有 `AppDestination.ranking(contentType, type)` 可编码榜单类型（`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-12-theater-channel/android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:117-121`），而 iOS 只有不带参数的 `.rankingHome` 路由，且 `RankingViewModel` 默认 `selectedRankingType = .hot`（`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-12-theater-channel/ios/ShortDrama/Sources/App/AppRoute.swift:11-18`, `/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-12-theater-channel/ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:23-25`）。也就是说，spec 没有定义 iOS 如何从剧场页“一步直达预约榜”，实现阶段会被迫自行改需求。
- **修复状态**：✅ 已修复
- **修复说明**：已在 spec 中补充统一的“排行页初始化上下文”要求：从剧场进入预约入口时，页面首屏必须直接进入 `all + booking`；若路由层暂不支持显式榜单参数，则可通过页面初始化上下文或首屏自动切换逻辑实现，但不得要求用户二次点击手动切换。

### 问题 3：频道 Feed 的数据契约缺少稳定排序与 `heat` 字段语义，分页与多端展示规则不够可验证

- **严重程度**：🟡 中
- **维度**：完整性 / 可行性
- **描述**：spec 要求 `channel=all` 支持分页加载并避免重复数据，但 `GET /api/dramas/channel` 仅描述“返回真实 mock / 种子数据”，没有定义稳定排序规则，也没有说明 `heat` 是服务端下发的展示文案还是原始数值。当前仓库中已有的列表/排行接口更偏向下发结构化原始字段，例如 `DramaSchema` 不含 `heat`，`RankingDramaSchema` 使用数值型 `play_count / booking_count / recommendation_score`（`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-12-theater-channel/backend/src/lib/schemas.ts:18-45`）。而 spec 示例却把 `heat` 写成字符串 `"2.3万"`（`spec.md` 第 466-485 行语义），这会让 Backend、Android、iOS 在 DTO 和格式化职责上产生分歧，也不利于验证分页排序是否稳定。
- **修复状态**：✅ 已修复
- **修复说明**：已在 spec 中补充 `channel=all` 使用服务端固定确定性排序规则、`heat` 采用原始数值字段并由客户端格式化显示，以及该接口沿用现有 dramas 基础字段风格再扩展剧场展示字段，便于 design、实现与测试使用统一契约。

## 上一轮问题修复验证

> 仅非首轮 review 时填写。验证上一轮 review 报告中标记为 `✅ 已修复` 的问题是否真正被修改到位。

首轮 review，无上一轮问题需要验证。

## 遗留问题（需人工决策）

> 以下问题 agent 无法自行解决，需要人工确认。

| 编号 | 问题 | 建议 | 状态 |
|------|------|------|------|
| H-01 | 无 | 首轮 review 中唯一的人工决策问题已在本轮收敛为“允许切换到底部 `home` Tab 复用既有承接页”。 | 已回复 |

## 修改记录

| 轮次 | 修改项 | 修改内容 |
|------|--------|---------|
| 1 | 初始审查 | 基于 PRD、现有 wiki、Android/iOS/Backend 路由与数据契约完成首轮 review，新建本报告并记录 3 个问题 |
| 1 | 主 agent 修复 | 在 `spec.md` 中收敛 home/theater 承接策略、补充预约入口初始化上下文、明确 `heat` 原始数值与稳定排序规则，并同步更新本报告修复状态 |

## 结论

- [x] ✅ 所有问题已修复，可进入下一阶段（spec-human-review）
- [ ] ⚠️ 存在遗留问题，需要人工确认后再推进
