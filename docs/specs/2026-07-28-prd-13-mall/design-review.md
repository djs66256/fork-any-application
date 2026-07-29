# 技术方案 Review：PRD-13 商城

> Review 日期：2026-07-28
> Review 循环：第 2 轮
> 审查者：AI Agent

## 审查结果总览

### Shared 设计 (design.md)

| 维度 | 检查项数 | 通过 | 问题 | 已修复 |
|------|---------|------|------|--------|
| 与 Spec 一致性 | 6 | 6 | 0 | 2 |
| 功能完整性 | 5 | 5 | 0 | 2 |
| API 完整性 | 5 | 5 | 0 | 0 |
| 数据模型一致性 | 4 | 4 | 0 | 1 |
| 边界与错误处理 | 6 | 6 | 0 | 2 |
| 安全考虑 | 4 | 4 | 0 | 1 |
| 性能考虑 | 4 | 4 | 0 | 0 |

### 平台设计 (design-{platform}.md)

| 平台 | 与 Spec 一致性 | 功能完整性 | 架构 | 文件变更 | API 调用 | 状态管理 | 测试策略 | 总体 |
|------|--------------|----------|------|---------|---------|---------|---------|------|
| Backend | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| iOS | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Android | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Web | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

## 发现的问题

### Shared 设计问题

无。

### 平台设计问题

无。

## 跨端一致性检查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| API 调用与 Shared 设计一致 | ✅ | `GET /api/mall/products`、`mall.openSearch`、`mall.requestLogin`、`mall.syncAuthState`、`mall.restoreContext` 在 shared 与各端方案中已统一。 |
| 数据模型各端一致 | ✅ | `MallProduct`、`MallSearchContext`、`MallLoginContext`、`MallHostAuthState` / `MallHostMessage` 的字段语义已在 shared 与平台方案中对齐。 |
| 共享逻辑覆盖 | ✅ | Native → H5 登录态同步、Native 搜索返回商城恢复、登录返回商城恢复、容器重建最低恢复保证均已在 Shared / Web / iOS / Android 中形成闭环。 |
| 错误处理策略一致 | ✅ | Backend 负责接口错误 contract，Web 负责 H5 页面状态机与 bridge 降级，iOS / Android 负责容器加载失败、bridge 解析失败与返回恢复失败，职责边界已清晰对齐。 |

## 上一轮问题修复验证

> 按用户要求，本轮额外验证了上一轮标记为 `🔄 已修复，待验证` 的 3 个重点问题。

| 问题编号 | 原问题摘要 | 所属方案 | 原修复状态 | 验证结果 | 说明 |
|---------|-----------|---------|-----------|---------|------|
| S-1 | Native → H5 登录态同步 contract 缺失 | design.md / design-web.md / design-ios.md / design-android.md | 🔄 已修复，待验证 | ✅ 已验证修复 | `design.md` 已新增 `MallHostAuthState` / `MallHostMessage`、`mall.syncAuthState` 与同步时机约束；Web 已补齐 `mall-host-sync.ts` 与 `useMallPage` 的宿主消息处理；iOS / Android 也都明确了初次加载、登录返回、前后台切换后的同步责任。 |
| S-2 | Native 搜索返回商城的恢复 contract 未闭环 | design.md / design-web.md / design-ios.md / design-android.md | 🔄 已修复，待验证 | ✅ 已验证修复 | Shared 已补齐 `MallSearchContext` 与 `mall.restoreContext(reason='search-return')`；Web / iOS / Android 均补齐了搜索返回后恢复商城承接、保持 mall tab 高亮以及容器重建时降级恢复首页首屏的约束。 |
| ios-1 | `MallContainerState` 规划落在 Domain 层 | design-ios.md | 🔄 已修复，待验证 | ✅ 已验证修复 | `MallContainerState` 已迁移到 `ios/ShortDrama/Sources/Features/Mall/Models/MallContainerState.swift`，并明确其为 Feature / Presentation 层模型；Domain 层仅保留 `MallLoginContext` 这类跨层上下文实体。 |

## 遗留问题（需人工决策）

> 以下问题 agent 无法自行解决，需要人工确认。

无。

## 结论

- [x] ✅ 所有问题已修复，可进入下一阶段（design-human-review）
- [ ] ⚠️ 存在遗留问题，需要人工确认后再推进
