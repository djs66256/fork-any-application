# Wiki 收录报告：PRD-04 搜索发现

> 收录日期：2026-07-27
> 对应需求：spec.md

## 收录内容

| wiki 文档 | 操作 | 变更章节 | 说明 |
|-----------|------|---------|------|
| `wiki/features/search-discovery/index.md` | 新建 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 新增 PRD-04 搜索发现功能文档，收录首页搜索入口、搜索发现页、搜索结果页、热搜榜、本地历史、快捷入口、deeplink 扩展与 Web 占位边界 |
| `wiki/features/deeplink/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 依赖关系 / 已知限制 / 修订历史 | 将 deeplink 能力扩展到搜索发现相关页面，补充 Android `player` 历史 alias 兼容和 iOS 不兼容旧 host 的差异 |
| `wiki/api/dramas.md` | 更新 | GET /api/dramas / GET /api/dramas/search / GET /api/dramas/hot-search / 修订历史 | 补充搜索与热搜接口 contract、query 校验、匹配规则、Top 10 热搜种子和大页码空结果行为 |
| `wiki/api/index.md` | 更新 | API 文档索引 | 将 Dramas API 描述扩展为覆盖首页列表、搜索结果与热搜榜接口 |
| `wiki/architecture/overview.md` | 更新 | 概述 / 架构设计 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史 | 将系统总览从 PRD-02 首页 Feed 扩展到 PRD-04 搜索发现架构，补充移动端搜索发现链路、Backend 搜索/热搜接口与 Web 占位范围 |
| `wiki/features/index.md` | 更新 | 功能域索引 | 新增搜索发现功能入口，并同步 deeplink 描述扩展到搜索相关页面 |

## 修订记录

- `wiki/revision/2026-07-27-prd-04-search-discovery.md` 已创建

## 校验结果

| 校验项 | 结果 | 说明 |
|--------|------|------|
| Mermaid 校验 | 通过 | 本轮新增/更新文档未使用 Mermaid 图；已确认 0 个 Mermaid 代码块，无需执行 `mermaid-validate` |
| wiki 交叉引用 | 通过 | 已对本轮涉及文档的相对链接与源码路径引用执行自动校验，未发现缺失目标 |
| 源码引用 | 已人工核对 | 关键事实均尽量标注源码路径与行号，来源以代码为准、spec/qa 为辅 |

## 收录结论

- [x] ✅ 关键实现事实已同步到 wiki
- [x] ✅ 已创建本次 revision 修订记录
- [x] ✅ Mermaid 与交叉引用校验已完成

本轮收录以代码为准、文档为辅，已同步 Backend / Android / iOS 的实现事实，并明确以下范围边界：

- 搜索历史仅在搜索请求成功后写入，空结果也写，失败不写；
- Backend 搜索当前为 `title + category` 不区分大小写 contains 匹配，超大页码返回 `200 + data=[]`；
- Android canonical routes 为 `search`、`search/result?query={query}`、`ranking`、`classification`、`new-releases`、`actors`，并兼容 `player` 历史 alias；
- iOS canonical routes 为 `.searchHome`、`.searchResult(query:)`、`.rankingHome`、`.classificationHome`、`.newReleases`、`.actorHub`；
- `mall` / `earn` 继续由 H5 承载，其他业务页当前优先由 Native 承接；
- Web `/search`、`/rankings` 仍为占位页，不属于本期真实搜索发现交付范围；
- 真机 / 模拟器黑盒执行未开展，因此 wiki 已记录“待补测”的限制，但不影响本轮代码事实收录。
