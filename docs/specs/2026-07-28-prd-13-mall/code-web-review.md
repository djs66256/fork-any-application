# 代码 Review：Web — PRD-13 商城

> Review 日期：2026-07-29

## 审查结果

| 维度 | 状态 | 说明 |
|------|------|------|
| 实现与 design 一致性 | ✅ | `/mall` 首页、商品占位详情页、bridge 分流、登录拦截与宿主恢复流程均已按 design-web.md/design.md 收口。 |
| 无硬编码常量 | ✅ | mall 路由、分页与 bridge 相关常量均集中在配置或 schema/seed 中，`api-client.ts` 不再依赖硬编码测试地址。 |
| 代码风格符合平台规范 | ✅ | `npm run lint` 通过，无新增 lint 错误。 |
| 错误处理完备 | ✅ | 首屏/追加失败、bridge 失败、非法 banner 目标、超时/网络/响应结构异常均有受控兜底文案。 |
| 性能无明显问题 | ✅ | 分页请求继续保留去重与乱序保护，商品卡与 banner 图已增加 lazy/async 提示。 |
| API 调用一致性 | ✅ | 商城列表调用统一通过 `web/src/lib/mall/api.ts` 与 `web/src/lib/api-client.ts`。 |
| 所有测试通过 | ✅ | `npm test` 全量通过（101/101）。 |
| 响应式与可访问性 | ✅ | 登录拦截层具备 dialog 语义与 Escape 关闭；列表结束态和反馈消息具备明确可读状态。 |
| Native 宿主恢复闭环 | ✅ | `login-return`、`search-return`、`container-recreated` 三类消息均已消费，且仅容器重建时重拉首屏。 |

## 变更文件审查

| 文件 | 审查结果 | 问题数 |
|------|---------|--------|
| `web/src/lib/api-client.ts` | ✅ | 0 |
| `web/src/lib/api-client.test.ts` | ✅ | 0 |
| `web/src/lib/config.ts` | ✅ | 0 |
| `web/src/lib/schemas.ts` | ✅ | 0 |
| `web/src/lib/mall/api.ts` | ✅ | 0 |
| `web/src/app/mall/page.tsx` | ✅ | 0 |
| `web/src/app/mall/product/[id]/page.tsx` | ✅ | 0 |
| `web/src/features/mall/MallPageScreen.tsx` | ✅ | 0 |
| `web/src/features/mall/MallProductPlaceholderScreen.tsx` | ✅ | 0 |
| `web/src/features/mall/hooks/useMallPage.ts` | ✅ | 0 |
| `web/src/features/mall/components/*` | ✅ | 0 |
| `web/src/features/mall/**/*.test.ts` | ✅ | 0 |
| `web/src/features/mall/**/*.test.tsx` | ✅ | 0 |

## 发现的问题

本轮修复并复审后，未发现新的遗留问题。

## 修复记录

| 轮次 | 修复项 |
|------|--------|
| 1 | 补齐 mall schema、config、API 封装、bridge 与宿主消息解析，并落地 `/mall` 与 `/mall/product/[id]` 页面委托。 |
| 1 | 修复 `api-client.ts` 的 base URL 解析与错误提取逻辑，保证测试环境与浏览器环境均可正确构造 URL。 |
| 1 | 修复商城登录拦截层的 dialog 语义与 Escape 关闭行为。 |
| 1 | 修复 banner 点击行为，按 `target_type + target_value` 执行搜索、商品、web 与占位分流。 |
| 1 | 修复 `container-recreated` 场景下的商城首页恢复，确保首屏重新拉取。 |
| 1 | 修复商品列表结束态，按 `hasNextPage` 隐藏加载更多并展示明确的列表结束文案。 |
| 1 | 修复用户可见错误文案，避免直接暴露网络/超时/解析等技术错误。 |
| 1 | 为商品卡与 banner 图补充 `loading="lazy"` 与 `decoding="async"`。 |
| 1 | 补充并回归验证 hook、页面、bridge、schema、config、api-client 等测试用例。 |

## 上一轮问题修复验证

| 问题编号 | 原问题摘要 | 原修复状态 | 验证结果 | 说明 |
|---------|-----------|-----------|---------|------|
| #1 | Banner click 忽略 banner contract | ✅ 已修复 | ✅ 已验证修复 | `MallPageScreen` 已传入完整 banner，`useMallPage` 按 banner contract 执行分流。 |
| #2 | `container-recreated` 未真正恢复首屏 | ✅ 已修复 | ✅ 已验证修复 | Hook 已消费恢复消息并在容器重建时触发首页重载。 |
| #3 | `api-client` GET 请求头与 base URL 处理不稳定 | ✅ 已修复 | ✅ 已验证修复 | GET 不再强制携带 JSON content-type，base URL 解析已通过相关单测覆盖。 |
| #4 | 用户可能看到原始技术错误信息 | ✅ 已修复 | ✅ 已验证修复 | `useMallPage` 已将超时、网络、4xx/5xx 与 Zod 异常映射为稳定用户文案。 |
| #5 | 列表最后一页仍显示“加载更多” | ✅ 已修复 | ✅ 已验证修复 | 商品列表已依据 `hasNextPage` 展示结束态，不再保留无效操作。 |
| #6 | 商品/横幅图片缺少延迟加载提示 | ✅ 已修复 | ✅ 已验证修复 | 商品卡与 banner 图片均已添加 lazy/async 属性。 |
| #7 | 匿名用户点击商品 banner 依赖当前列表是否已加载该商品 | ✅ 已修复 | ✅ 已验证修复 | banner 商品点击已直接基于 `target_value` 建立登录上下文，不再依赖当前页商品列表。 |

## 遗留问题（需人工决策）

无。

## 结论

- [x] ✅ 所有问题已修复，代码质量合格
- [ ] ⚠️ 存在遗留问题，需人工确认
