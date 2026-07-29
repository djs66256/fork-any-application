# 功能域索引

- [应用壳 (App Shell)](app-shell/index.md) — 各端应用启动入口、移动端 5 Tab 容器，以及首页频道承载 Feed、菜单抽屉、搜索发现、排行、分类，“赚钱”频道承载 H5 容器与登录/任务回流，“我的”频道承载登录 / 设置等真实 Native 子页面的关系
- [认证体系 (Auth)](auth/index.md) — 移动端手机号验证码登录、自动注册、会话恢复 / refresh / logout、Profile 登录后态、排行预约登录拦截、评论写操作当前对齐的鉴权基线，以及赚钱 H5 容器的宿主登录态同步
- [首页信息流 (Homepage Feed)](homepage-feed/index.md) — Android / iOS 首页首屏 Feed、Backend `GET /api/dramas` 契约、卡片到播放/详情页主链路，以及首页评论入口与页面内评论容器
- [剧场频道 (Theater)](theater/index.md) — Android / iOS 独立剧场一级 Tab、`GET /api/dramas/channel`、8 频道切换、剧场快捷入口与 `play` 主路径复用
- [搜索发现 (Search Discovery)](search-discovery/index.md) — 首页搜索入口与剧场搜索入口延伸出的搜索发现页、搜索结果页、热搜/历史、快捷入口与搜索 API 主链路
- [排行体系 (Ranking)](ranking/index.md) — 搜索发现页与剧场快捷入口的排行承接、双层 Tab 榜单浏览、分页、预约拦截、可选鉴权榜单与登录后继续操作
- [分类浏览 (Classification)](classification/index.md) — 搜索发现页分类入口、固定三维度标签矩阵、`GET /api/dramas/tags`、标签点击复用搜索结果页与 Native / Web 范围边界
- [赚钱中心 (Earn Center)](earn/index.md) — Web `/earn` H5 页面、Android / iOS 原生容器接入、earn 专属 bridge / host sync、代表性任务播放回流与奖励闭环
- [健康检查 (Health Check)](health-check/index.md) — 后端服务运行状态监控端点
- [数据模型 (Data Models)](data-models/index.md) — 核心数据实体定义、首页卡片字段、排行扩展字段、评论契约与分类标签契约
- [深链 (Deeplink)](deeplink/index.md) — 自定义 URL Scheme 唤起应用，含 `ranking` / `classification` 等发现链路入口与 Android `player` 兼容
- [播放器](video-player/index.md) — 跨端播放链路、菜单最近在看入口、首页 Feed / 排行卡片入口、播放器评论容器与 player API / comments API 现状，以及赚钱代表性任务复用原生播放主路径后的回流语义
- [评论能力 (Comments)](comments/index.md) — PRD-09 评论系统首版落地结果、Feed / Player 入口、comments API、登录恢复上下文与已知限制
- [管理平台 (Admin Panel)](admin-panel/index.md) — Web 端内部管理后台，提供短剧/剧集内容管理、用户角色分配、仪表盘概览与 RBAC 三级权限控制

---
*本文档由 llm-wiki skill 自动维护。*
