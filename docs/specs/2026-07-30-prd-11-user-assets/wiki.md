# Wiki 收录报告：PRD-11 个人资产管理

> 收录日期：2026-07-30
> 对应需求：spec.md

## 收录内容

| wiki 文档 | 操作 | 变更章节 | 说明 |
|-----------|------|---------|------|
| `wiki/features/user-assets/index.md` | 新建 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 新增个人资产管理主功能文档，收录菜单“我的预约”真实页、`GET /api/users/me/bookings`、匿名登录承接与 booking route 回流、双 Tab `online/upcoming` + summary、“我的下载”占位策略与 Web skipped 边界 |
| `wiki/api/user-assets.md` | 新建 | 概述 / `GET /api/users/me/bookings` / 与其它接口的关系 / 修订历史 | 新增用户资产 API 文档，收录 booking assets 读取接口的鉴权、query 默认值、`{ data, pagination, summary }` contract、状态映射、脏数据过滤与错误码语义 |
| `wiki/features/index.md` | 更新 | 功能域索引 | 新增“个人资产管理 (User Assets)”入口，并同步修正 Auth 摘要 |
| `wiki/api/index.md` | 更新 | API 文档索引 | 新增“个人资产 (User Assets)” API 入口 |
| `wiki/features/auth/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 修订历史 | 补充预约资产页登录承接、booking route 回流与 booking assets 接口在统一鉴权体系中的位置 |
| `wiki/features/app-shell/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 将应用壳从“菜单占位承接”同步为“菜单 booking 真实页 + downloads 占位页”的当前口径，并补充壳层 close-menu-then-navigate / 登录回流职责 |
| `wiki/architecture/overview.md` | 更新 | 概述 / 架构设计 / 当前首页发现、签到、消息、预约资产、商城、赚钱与账号承载结构 / 当前认证、签到、消息、预约资产、评论、商城与赚钱能力分层现状 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史 | 将系统总览同步到 PRD-11 口径，补充 booking assets route、移动端真实 booking 页面、服务端 summary 聚合和 Web skipped / downloads 占位边界 |

## 修订记录

- `wiki/revision/2026-07-30-prd-11-user-assets.md` 已创建

## 校验说明

- 本轮收录以当前 master 中的 Backend / Android / iOS 真实实现为准，以 PRD 与 QA 文档为辅；当文档与设计稿存在差异时，一律以代码实现为准。
- 已按 llm-wiki 规范检查本轮收录文档中的 Markdown 链接与显式源码路径引用；当前新增 / 更新文档均未包含 Mermaid 图表，因此 Mermaid 校验结果为 0 图表 / 0 错误。
- 已重点回链并核对以下 PRD-11 关键事实：`GET /api/users/me/bookings` 使用 `requireAuthContext(...)` 强制鉴权；query 默认值由 `BookingAssetQuerySchema` 收口；服务端统一返回 `{ data, pagination, summary }` 且 `pagination` 保持 snake_case；状态映射固定为 `announced -> upcoming`、`ongoing/completed -> online`；Android / iOS 菜单“我的预约”进入真实 booking 页面；匿名态登录成功后回 booking route；“我的下载”继续为占位；Web 本期 skipped。
- QA 部分未伪造真实通过结论：本轮仅引用 `docs/specs/2026-07-30-prd-11-user-assets/qa-test.md` 中已明确标注的“仅完成用例设计、设备/模拟器黑盒执行阻塞 / 未执行”状态。
- 未新增 PRD-11 专属 decision 文档：当前变更已可由 feature / api / architecture / revision / inclusion report 覆盖，未发现需要独立沉淀的长期架构分歧。

## Wiki 验证报告

> 验证时间：2026-07-30
> 验证范围：8 个文件

### Mermaid 语法验证

| 文件 | Mermaid 图表数 | 合法 | 非法 |
|------|---------------|------|------|
| `wiki/features/user-assets/index.md` | 0 | 0 | 0 |
| `wiki/api/user-assets.md` | 0 | 0 | 0 |
| `wiki/features/index.md` | 0 | 0 | 0 |
| `wiki/api/index.md` | 0 | 0 | 0 |
| `wiki/features/auth/index.md` | 0 | 0 | 0 |
| `wiki/features/app-shell/index.md` | 0 | 0 | 0 |
| `wiki/architecture/overview.md` | 0 | 0 | 0 |
| `wiki/revision/2026-07-30-prd-11-user-assets.md` | 0 | 0 | 0 |

非法详情：无。

### 交叉引用验证

| 文件 | 验证范围 | 结果 |
|------|---------|------|
| `wiki/features/user-assets/index.md` | API 文档链接、源码路径引用 | 有效 |
| `wiki/api/user-assets.md` | route / schema / service / repository / test 源码路径引用 | 有效 |
| `wiki/features/index.md` | `user-assets/index.md` 功能域入口链接 | 有效 |
| `wiki/api/index.md` | `user-assets.md` API 入口链接 | 有效 |
| `wiki/features/auth/index.md` | `../../api/user-assets.md` 链接、booking 相关源码路径引用 | 有效 |
| `wiki/features/app-shell/index.md` | `../../api/user-assets.md` 链接、booking 相关源码路径引用 | 有效 |
| `wiki/architecture/overview.md` | booking 相关源码路径引用 | 有效 |
| `wiki/revision/2026-07-30-prd-11-user-assets.md` | 变更文件与主要来源路径引用 | 有效 |

无效引用详情：无。

### 验证结论

- Mermaid 语法：✅ 全部通过
- 交叉引用：✅ 全部有效（按 llm-wiki 规范校验 Markdown 内部链接与显式源码文件引用）

## 收录结论

- [x] 已同步 PRD-11 个人资产管理直接影响的 feature / api / index / revision 文档
- [x] 已补齐 `GET /api/users/me/bookings` 的独立 API 文档，以及菜单 booking 页面相关的 app-shell / auth / architecture 增量更新
- [x] 已在文档中明确 Web skipped、"我的下载"占位策略，以及 QA 未执行 / 阻塞事实

当前仍保留的实现限制：

- Web 本期仍不提供用户端“我的预约 / 我的下载”真实页面，也不接入 booking assets API client。
- “我的下载”在 Android / iOS 仍是 Native 占位页，不包含真实下载资产、离线包或下载任务状态。
- iOS 代码中仍保留历史 `.booking` placeholder enum case，但菜单真实入口已经切换为 `.bookingAssets`。
- 设备 / 模拟器黑盒测试本轮未执行；当前 wiki 结论主要来自源码、Backend 自动化测试与 QA 用例设计文档。
