# Wiki 收录报告：PRD-13 商城

> 收录日期：2026-07-29
> 对应需求：spec.md

## 收录内容

| wiki 文档 | 操作 | 变更章节 | 说明 |
|-----------|------|---------|------|
| `wiki/features/mall/index.md` | 新建 | 全文初始创建 | 新增商城频道功能文档，收录 H5 承载策略、Web `/mall` 与 `/mall/product/[id]`、Android/iOS 容器接入、搜索/登录 bridge、商品 Feed、宿主恢复语义与已知限制 |
| `wiki/api/mall.md` | 新建 | 全文初始创建 | 新增 `GET /api/mall/products` API 文档，收录 query 默认值、商品字段契约、固定 25 条 seed 数据、超大页码空结果与统一错误处理 |
| `wiki/features/index.md` | 更新 | 功能域索引 | 新增“商城频道”入口，并补充搜索发现与认证体系对商城搜索/登录承接的说明 |
| `wiki/api/index.md` | 更新 | API 文档索引 | 新增 Mall API 入口 |
| `wiki/features/app-shell/index.md` | 更新 | 功能概述、核心逻辑、iOS 承载、已知限制、修订历史 | 把商城从占位一级频道修正为真实 H5 容器入口，并明确当前只剩赚钱频道仍为占位 |
| `wiki/features/search-discovery/index.md` | 更新 | 功能概述 | 补充商城顶部搜索入口复用既有搜索能力：Native 模式走 bridge，浏览器模式 fallback 到 `/search` |
| `wiki/features/auth/index.md` | 更新 | 功能概述、入口与路由、依赖关系、修订历史 | 补充商城匿名商品点击复用统一 Native 登录承接与 `returnTarget=/mall` 返回语义 |
| `wiki/architecture/overview.md` | 更新 | 概述、整体架构、承载结构、设计决策、已知限制、修订历史 | 将 PRD-13 纳入系统总览，补充商城 H5 容器、`GET /api/mall/products`、搜索/登录 bridge 与跨端承载边界 |
| `wiki/revision/2026-07-29-prd-13-mall.md` | 新建 | 全文初始创建 | 记录本次 wiki 收录涉及的文档、章节与主要代码来源 |

## 修订记录

- `wiki/revision/2026-07-29-prd-13-mall.md` 已创建

## 收录结论

- [x] ✅ 所有本次 PRD-13 商城相关核心变更已同步到 wiki
- [ ] ⚠️ 部分内容因信息不足未收录（见下方说明）

已在 wiki 中明确记录以下当前实现限制，而不是省略不写：

- Native → H5 宿主消息当前由 Android/iOS 注入 `CustomEvent('mall.syncAuthState' / 'mall.restoreContext')`，但 Web `subscribeMallHostMessages()` 当前监听的是 `message` 事件；该实现通道差异已作为已知限制收录到 `wiki/features/mall/index.md` 与系统总览，不再把设计意图误写成“已完全闭环”。
- QA 文档已产出，但设备/模拟器黑盒执行仍按 workflow 规则降级为跳过，因此 wiki 中对黑盒验证结论保持谨慎表述。
