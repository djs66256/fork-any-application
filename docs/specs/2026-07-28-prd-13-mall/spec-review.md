# 需求 Review：PRD-13 商城

> Review 日期：2026-07-28
> Review 循环：第 1 轮
> 审查者：AI Agent

## 审查结果总览

| 维度 | 检查项数 | 通过 | 问题 | 已修复 |
|------|---------|------|------|--------|
| 完整性 | 12 | 10 | 2 | 2 |
| 边界与错误处理 | 8 | 7 | 1 | 1 |
| 一致性（与 wiki） | 7 | 5 | 2 | 2 |
| 可行性 | 6 | 4 | 2 | 2 |
| 平台覆盖 | 4 | 3 | 1 | 1 |
| 术语与范围 | 5 | 4 | 1 | 1 |

## 发现的问题

### 问题 1：商城登录拦截的承载形态与返回契约仍未收敛，当前不足以进入 design 阶段

- **严重程度**：🔴 高
- **维度**：完整性 / 一致性 / 可行性
- **描述**：spec 一方面要求匿名用户点击商品卡后“在商城上下文内触发登录拦截，并在返回后保留商城 tab 高亮与页面状态”（`spec.md:28-29`, `spec.md:305-334`），另一方面又在待澄清问题中保留了两个完全不同的落地分支：直接接入真实登录页，或先接现有 Native 登录占位承接页（`spec.md:556-558`）。这两个分支与现有代码/文档的语义差异很大：
  - iOS 现有排行登录拦截只是页内 `alert`，并不会进入统一登录承接（`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift:61-67`）。
  - Android 现有排行虽然产出了 `RequireLogin(returnRoute)`，但 `NavGraph` 当前直接忽略该 effect，未真正接入登录流（`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:193-203`, `/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:222-230`）。
  - 当前唯一成型的“登录承接页”仍是首页链路下的 Native placeholder：Android `menu/login`、iOS `.menuPlaceholder(kind: .login)`，它们都不属于 mall 业务域（`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:388-409`, `/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPlaceholderView.swift:3-26`）。
  因此，spec 目前没有明确回答以下 design 级关键问题：登录拦截究竟是 H5 页内覆盖层、Native modal、还是跳出现有 home-owned 登录占位页；登录成功/取消后的返回目标由谁保存；若暂时复用占位页，如何仍满足“商城上下文内”的产品语义。现在的描述足够表达意图，但还不足以直接拆 design。
- **修复状态**：✅ 已修复
- **修复说明**：已在 spec 中收敛单一路径：首版默认采用“商城 H5 页内登录拦截覆盖层 + bridge 打开统一 Native 全屏登录承接页”的组合；并补齐了 `MallLoginContext` 最小字段（`source=mall`、`productId`、`returnTarget=/mall`）、取消/失败/关闭/成功后的返回契约，以及“不得直接复用 home 菜单 `menu/login` 语义替代商城登录拦截”的约束。

### 问题 2：搜索入口的跨端归属未写清，导致 Web 涉及范围与 App Shell 现状之间仍有语义缺口

- **严重程度**：🟡 中
- **维度**：一致性 / 平台覆盖 / 术语与范围
- **描述**：spec 将 Web 标注为涉及平台是正确的，因为商城首页与商品详情占位页本身由 H5 提供（`PRODUCT.md:22-25`, `spec.md:23-30`, `spec.md:76-83`）。但在搜索入口上，spec 只写了“商城通过约定的桥接能力进入现有搜索页（复用 PRD-04 搜索能力）”（`spec.md:249-253`），没有进一步说明 Native 与浏览器/本地开发两种承载形态下的归属差异。现有 App Shell 中，iOS `.searchHome` 固定属于 `home` tab（`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/ios/ShortDrama/Sources/App/AppRoute.swift:26-40`, `/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/ios/ShortDrama/Sources/App/NavigationRouter.swift:51-69`），Android `search` 也注册在 HOME graph（`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:187-208`）；而 Web 当前 `/search` 仍只是占位路由（`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/web/src/app/search/page.tsx:1-9`）。如果不在 spec 中写清：1）Native 容器内点击搜索是否允许切到 home-owned 搜索页；2）浏览器直接访问 `/mall` 时点击搜索如何降级；3）从搜索页返回时对 mall tab / mall 上下文的最低保证是什么，design 阶段会在路由、桥接和状态恢复上自行补需求。
- **修复状态**：✅ 已修复
- **修复说明**：已在 spec 中补齐搜索入口的跨端归属约束：Native 容器模式下通过 bridge 打开现有 Native 搜索页，浏览器 / 本地开发模式下默认跳转 `/search`；同时明确了返回商城后的最低保证是可重新回到商城首页且商城 tab 高亮正确。

### 问题 3：Banner seed/config 只收敛到“不要写在 Native”，但仍缺少首版默认归属与最小契约，容易把实现压力下放到 design 阶段临时拍板

- **严重程度**：🟡 中
- **维度**：完整性 / 可行性
- **描述**：spec 已经正确修正了原始 PRD / subtasks 中“硬编码图片 URL”的方向，并强调不在 iOS / Android 客户端内硬编码 banner 图片 URL（`spec.md:42`, `spec.md:62`, `spec.md:480-482`）；但当前正文仍只停留在“服务端配置或 H5 集中配置，design 阶段继续收敛”，且把最终载体作为待澄清问题保留（`spec.md:556-557`）。由于 banner 是商城首页首屏固定区块之一（`spec.md:26`, `spec.md:108-113`），如果 spec 不给出首版默认归属与最小字段契约，Web / Backend 设计阶段仍可能走向两套方案：一边把 seed 直接写死在 H5 组件里，一边再补服务端配置，最终反而把“禁止硬编码常量”的约束退化成“只是不写到 Native”。
- **修复状态**：✅ 已修复
- **修复说明**：已在 spec 中明确首版默认由 Web H5 集中配置模块单点持有 banner seed/config，并补齐最小字段契约 `id / image_url / target_type / target_value / sort_order`；同时明确禁止在 Native 客户端或页面组件内内联 banner 图片 URL。

## 上一轮问题修复验证

> 仅非首轮 review 时填写。验证上一轮 review 报告中标记为 `✅ 已修复` 的问题是否真正被修改到位。

首轮 review，无上一轮问题需要验证。

## 遗留问题（需人工决策）

> 以下问题 agent 无法自行解决，需要人工确认。

| 编号 | 问题 | 建议 | 状态 |
|------|------|------|------|

## 修改记录

| 轮次 | 修改项 | 修改内容 |
|------|--------|---------|
| 1 | 初始审查 | 基于 spec-review 规范、`PRODUCT.md`、`wiki/features/app-shell/index.md`、商城/登录/排行相关 PRD 与 Android/iOS/Web 现状完成首轮 review，记录 3 个问题，其中 1 个初始判定为需人工决策。 |
| 1 | 主 agent 修复 | 已在 spec 中收敛登录拦截默认路径、搜索入口跨端归属、banner seed/config 默认归属与最小字段契约，并同步消除待澄清项。 |

## 结论

- [x] ✅ 所有问题已修复，可进入下一阶段（spec-human-review）
- [ ] ⚠️ 存在遗留问题，需要人工确认后再推进
