# 技术方案 Review：PRD-02 首页信息流

> Review 日期：2026-07-25
> Review 循环：第 1 轮
> 审查者：AI Agent

## 审查结果总览

### Shared 设计（design.md）

| 维度 | 检查项数 | 通过 | 问题 | 已修复 |
|------|---------|------|------|--------|
| 与 Spec 一致性 | 6 | 6 | 0 | 0 |
| Canonical Contract 收口 | 4 | 4 | 0 | 0 |
| 跨端共享逻辑 | 5 | 5 | 0 | 0 |
| 边界与错误处理 | 5 | 5 | 0 | 0 |
| 范围边界控制 | 4 | 4 | 0 | 0 |
| 测试与可实现性约束 | 3 | 3 | 0 | 0 |

### 平台设计（design-{platform}.md）

| 平台 | 与 Spec 一致性 | Canonical Contract | 状态模型 | 路由映射 | 测试策略 | 总体 |
|------|--------------|--------------------|---------|---------|---------|------|
| Backend | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| iOS | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Android | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

## 发现的问题

> 首轮审查未发现当前仍存在的 🔴 阻塞、🟡 关注或需人工决策问题。

## 跨端一致性检查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| Canonical contract 一致 | ✅ | Shared、Backend、iOS、Android 均收口到 `GET /api/dramas` + query `page/pageSize` + 响应 `{ data, pagination }`；已明确不再延续 iOS `/api/v1/dramas` 和 Android `page_size` 旧契约 |
| MVP 范围一致 | ✅ | Shared 与三端方案均明确本期只做首页首屏第一页，不做下拉刷新、自动加载更多或第二页消费 |
| 首页形态一致 | ✅ | Shared、iOS、Android 均明确首版为常规列表卡片，不做沉浸式 Feed |
| 播放/详情路由映射一致 | ✅ | iOS、Android 均采用 `drama.id` 映射到播放与详情入口；共享方案明确播放走 `play/:id`，详情走 `detail/:id` |
| 搜索/榜单导流边界一致 | ✅ | Shared 与平台方案均未引入搜索入口、榜单入口或其它导流能力，符合 spec 的 MVP 边界 |
| Web 范围边界一致 | ✅ | Shared 明确 Web 本期不做首页 Feed；平台拆分也仅包含 Backend / iOS / Android，未误扩展 Web 首页实现 |
| Backend schema / mock / tests 收口完整 | ✅ | Backend 方案已覆盖 `DramaSchema` 字段统一、mock 数据集、分页边界、非法参数、501 回归等测试策略，满足首页 Feed 契约与 mock 落地要求 |
| iOS 迁移路径清晰 | ✅ | iOS 方案已明确从 `/api/v1/dramas` + 包裹响应迁移到 `/api/dramas` + `{ data, pagination }`，并同步收口 query 为 `page/pageSize` |
| Android 迁移路径清晰 | ✅ | Android 方案已明确将 `page_size` 迁移为 `pageSize`，并以 `ApiService -> DataSource -> Repository -> ViewModel` 链路接入首页 |
| 错误态 / 空态 / 重试语义一致 | ✅ | iOS / Android 均显式区分 loading / content / empty / error，空列表进入空态、异常进入错误态、重试采用手动触发；Backend 也明确空列表返回 `data=[]`、非法参数为 400、异常由统一错误结构返回，语义兼容客户端状态机 |

## 可实现性与测试策略检查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| Backend 可实现性 | ✅ | 现有代码已具备 `/api/dramas` route、service、mock repository、Vitest 测试骨架；方案是在既有四层结构上补齐 schema、mock 和测试，落地成本清晰 |
| iOS 可实现性 | ✅ | 现有代码已具备 `HomeViewModel`、`FetchDramasUseCase`、`DramaRemoteDataSource`、`NavigationRouter` 等基础设施；方案是在既有 MVVM + Clean Architecture 上做协议迁移与首页 UI 升级 |
| Android 可实现性 | ✅ | 现有代码已具备 `GetDramasUseCase`、DTO / Repository / ApiService 链路及导航常量；方案是在既有 Compose + ViewModel 结构上接入首页 Feed，改动边界清晰 |
| Backend 测试策略 | ✅ | 已覆盖默认分页、第一页成功态、多页分页、大页码、非法参数、未实现 POST 回归，满足接口契约与分页校验要求 |
| iOS 测试策略 | ✅ | 已覆盖 ViewModel 的 success / empty / error / retry，并补充 Data 层 endpoint/query/解码测试方向，足以覆盖本次协议迁移的主风险点 |
| Android 测试策略 | ✅ | 已覆盖 ViewModel 状态机、DTO 映射、query 命名收口与路由联通性，能够验证首页主链路与契约收口 |

## 上一轮问题修复验证

> 首轮 review，无上一轮问题需要验证。

## 遗留问题（需人工决策）

无。

## 结论

- [x] ✅ 方案与 spec 保持一致，重点约束均已收口：`/api/dramas`、`page/pageSize`、`{ data, pagination }`、首页首屏第一页、常规列表卡片、`drama.id -> play/:id`、不做搜索/榜单导流、Web 本期不做首页 Feed
- [x] ✅ Backend / iOS / Android 方案之间无当前仍存在的跨端契约冲突，错误态、空态、重试语义可对齐实现
- [x] ✅ 可实现性与测试策略完整，可进入 design-human-review
- [ ] ⚠️ 存在遗留问题，需要人工确认后再推进
