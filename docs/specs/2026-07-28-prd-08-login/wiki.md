# Wiki 收录报告：PRD-08 登录闭环

> 收录日期：2026-07-29
> 对应需求：spec.md

## 收录内容

| wiki 文档 | 操作 | 变更章节 | 说明 |
|-----------|------|---------|------|
| `wiki/api/auth.md` | 新建 | 概述 / OTP 请求 / 登录会话 / refresh / me / logout / 与受保护业务接口的关系 / 修订历史 | 新增 PRD-08 认证 API 文档，收录手机号验证码登录、自动注册、会话恢复 / refresh、幂等登出与 bearer auth contract |
| `wiki/api/index.md` | 更新 | API 文档索引 | 新增认证 API 入口，并补充 Auth 文档覆盖范围 |
| `wiki/api/dramas.md` | 更新 | `GET /api/dramas/rankings` / `POST /api/dramas/:id/book` / 修订历史 | 将排行与预约接口的认证语义更新为真实 bearer access token 与 `DramaSupabaseRepository()` 运行时 |
| `wiki/features/auth/index.md` | 新建 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 新增认证体系功能文档，覆盖移动端登录、自动注册、会话恢复 / refresh / logout、“我的”频道与排行预约登录拦截 |
| `wiki/features/index.md` | 更新 | 功能域索引 | 新增“认证体系 (Auth)”入口，并同步更新应用壳摘要 |
| `wiki/features/app-shell/index.md` | 更新 | 功能概述 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 把应用壳从发现链路容器更新为同时承载“我的”频道登录 / 设置 / 回跳的真实容器 |
| `wiki/features/ranking/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 将排行体系同步到 PRD-08 的真实登录拦截、可选 / 强制 bearer 鉴权、Supabase repository 运行时与自动化证据 |
| `wiki/architecture/overview.md` | 更新 | 概述 / 架构设计 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史 | 将系统总览从 PRD-06 扩展到 PRD-08 登录闭环，补充“我的”频道、Auth API、启动恢复与排行预约登录拦截 |

## 修订记录

- `wiki/revision/2026-07-28-prd-08-login.md` 已创建

## 校验说明

- 本轮收录以当前 worktree 中的 Backend / Android / iOS 真实代码为准，以 PRD、QA 文档与已有 wiki 为辅，所有新增事实均以 `path:line` 形式回链到源文件。
- 已核对 `wiki/features/ranking/index.md` 中 Android 自动化证据：`RankingViewModelTest` 的 `T-09 anonymous booking emits require login and skips api call` 确认了未登录预约时发出 `RequireLogin("ranking?contentType=all&type=booking")` 且不会调用预约 API。
- 已把过期的 `mock repository` / `skeleton auth` 叙述从排行文档与系统总览中替换为当前真实运行事实：发现接口仍多为 mock 数据，排行 / 预约与认证闭环已切换到真实 Supabase / bearer auth 路径。
- 文档中仍无法通过本轮代码与 QA 产物直接确认的结论，没有写成“已验证事实”；设备级体验相关结论继续以 `qa-test.md` 中的降级说明为准。

## 收录结论

- [x] ✅ 所有本次 feature 直接影响的 wiki 内容已同步
- [x] ✅ 新增认证体系功能文档与 Auth API 文档已建立索引与修订记录
- [x] ✅ 系统总览、应用壳、排行体系已从 PRD-05 / PRD-06 口径同步到 PRD-08 登录闭环口径

当前仍保留的实现限制：

- Web 端不参与 PRD-08 用户端登录闭环，仍无与移动端对等的“我的”频道登录页、真实排行页与真实分类页。
- Backend 当前首页 / 搜索 / 热搜 / 分类 tags 仍主要来自 `DramaMockRepository`；只有排行 / 预约与 auth 已切换到真实 Supabase 路径。
- iOS 排行登录成功后只回到 `.rankingHome`，不显式恢复更细粒度的 `contentType/rankingType` query；Android 会保留完整 `ranking?...` returnRoute。
- 剧场 / 商城 / 赚钱频道与真实 H5 容器仍未在移动端接入。
- 设备 / 模拟器黑盒测试在本轮 workflow 中按规范降级，当前 wiki 证据仍主要来自代码、自动化测试与 QA 文档。
