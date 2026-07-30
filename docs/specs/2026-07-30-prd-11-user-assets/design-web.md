# Web 端技术方案：PRD-11 个人资产管理

> 创建日期：2026-07-30
> 对应共享方案：design.md
> 对应需求：spec.md
> 平台结论：**skipped / 本期不实现 Web 端个人资产页**

---

## 1. 架构设计

PRD-11 的共享方案已经明确：本期“个人资产管理”首版只覆盖 **Backend + iOS + Android**，不新增 Web 用户端资产页，也不把“我的预约 / 我的下载”改为 H5 承载。

因此 Web 端在本期的职责不是新增页面或接入 `GET /api/users/me/bookings`，而是**正式记录范围外结论**，避免后续在 design-review、plan 或 coding 阶段误把 Web 纳入实现范围。

```text
PRD-11 个人资产管理
├── Backend
│   └── 新增 GET /api/users/me/bookings
├── iOS
│   └── 菜单“我的预约”升级为真实 Native 页面
├── Android
│   └── menu/booking 升级为真实 Native 页面
└── Web
    └── skipped（不新增 H5 资产页，不修改现有 web route）
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `web/src/app/` | 不变 | 不新增 `/assets`、`/bookings`、`/downloads` 等页面，也不新增任何 booking 相关 route |
| `web/src/features/` | 不变 | 不新增 booking/downloads feature，也不新增资产页入口组件 |
| `web/src/lib/api-client.ts` | 不变 | 本期 Web 不消费 `GET /api/users/me/bookings`，不新增 booking assets API client |
| `web/src/lib/schemas.ts` | 不变 | 不新增 booking assets Web DTO / schema |
| `web/tests/` | 不变 | 无 booking assets Web 入口，因此不新增页面、API、schema 或交互测试 |
| `web/src/styles/` | 不变 | 无新增资产页样式 |

### 1.2 范围判定依据

1. `spec.md` 已明确：**Web 用户端不实现个人资产管理页面**。
2. `design.md` 已明确：**PRD-11 不引入新的一级频道，也不把资产页做成 H5**。
3. 当前产品承载策略记忆已明确：商城与赚钱走 H5，其它业务页面默认按 Native 规划；“我的预约”不在 H5 承载范围内。
4. “我的下载”本期也只是移动端菜单占位策略收口，不对应 Web 页面。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `docs/specs/2026-07-30-prd-11-user-assets/design-web.md` | 新增 | 记录 Web skipped 决策与边界，供 design-review / plan / coding 阶段引用 |

> 除本设计文档外，`web/` 目录本期无代码变更目标。

---

## 3. 组件设计

本期 skipped，无新增页面组件、业务组件或占位组件。

### 3.1 组件层级树

```text
无新增 Web 组件
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | Props 接口 |
|---------|------|------|-----------|
| 无 | — | — | — |

### 3.3 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 无新增 | — | Web 不承接本期个人资产页 |

### 3.4 响应式设计

无新增需求。

### 3.5 无障碍（A11y）

无新增需求。

---

## 4. 状态管理方案

本期 skipped，无新增状态管理。

| 维度 | 结论 | 说明 |
|------|------|------|
| 页面状态 | 不新增 | 无 booking assets Web 页面 |
| 服务端状态 | 不新增 | 不拉取 booking assets 接口 |
| 登录态联动 | 不新增 | 登录承接仅发生在移动端 Native 页面 |
| URL 状态 | 不新增 | 不新增 Web 路由 |

---

## 5. 路由设计

### 5.1 路由清单

| 路径 Pattern | 页面组件 | 参数 | 认证守卫 | 懒加载 | 说明 |
|-------------|---------|------|---------|--------|------|
| 无新增 | — | — | — | — | PRD-11 不新增 Web 个人资产路由 |

### 5.2 路由层级

```text
无新增 Web 路由
```

### 5.3 路由守卫

无新增路由，因此无新增守卫逻辑。

### 5.4 数据预取与加载

无新增页面，因此无新增预取策略。

---

## 6. API 调用层设计

Web 端本期不接入 `GET /api/users/me/bookings`。由于本期没有任何 Web route、页面入口或 feature 会消费这项能力，因此同时明确：**不新增 booking assets API client，不新增 booking assets schema，也不新增围绕该接口的重试或错误处理逻辑。**

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | 不变 | 继续服务于现有 Web 功能；不为 booking assets 新增调用 |
| 请求封装 | 不变 | 不新增 booking assets API 封装 |
| 响应校验 | 不变 | 不新增 booking assets schema |

### 6.2 请求重试策略

无新增请求，因此无新增重试策略，也无针对 booking assets 的额外错误处理分支。

---

## 7. SSR / CSR 策略

本期 skipped，无新增 Web 页面，因此不存在新的 SSR / CSR 选择。

---

## 8. 性能优化

本期 skipped，无新增性能项。

---

## 9. 配置与环境

本期 skipped，无新增 Web 环境变量、配置项或 feature flag。

---

## 10. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| 无新增 | — | — | — | — |

---

## 11. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Web 端实现方式 |
|---------|---------------|---------------|
| 资产页承载策略 | 预约资产页不做成 H5 | skipped；由 iOS / Android Native 页面承接 |
| 我的预约真实页 | 移动端菜单升级为真实页面 | Web 不实现对应 route |
| 我的下载占位策略 | 移动端保留 placeholder | Web 不新增下载占位页 |
| 登录承接 | 匿名进入 booking 后在原上下文登录 | 仅移动端适用，Web 不接入 |
| `GET /api/users/me/bookings` | 提供给移动端资产页消费 | Web 本期不消费 |

---

## 12. 边界与错误处理

### 12.1 边界结论

| 场景 | 结论 | 说明 |
|------|------|------|
| 用户在 Web 端寻找“我的预约” | 不提供对应页面 | 不在本期范围，且不新增任何 Web route |
| 用户在 Web 端寻找“我的下载” | 不提供对应页面 | 不在本期范围，不新增下载占位页 |
| 后端已新增 booking assets 接口 | Web 不跟进消费 | 仅为移动端 Native 页面服务；不新增 api-client 或 schema |
| 后续若要做 Web 个人资产页 | 需新 PRD | 重新评估承载策略、登录态、路由、UI 与测试方案 |

### 12.2 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| design-review 误把 Web 当作必做平台 | 文档流程 | 🟡 | 中 | 显式输出本 skipped 文档并在 workflow 中标记 `web=skipped` | 若仍有歧义，以 `spec.md` 与 `design.md` 为准 |
| 后续 plan/coding 误创建 Web 资产页 | Web 代码范围 | 🟡 | 中 | 在 design-web 中明确“无代码变更目标” | 删除误建方案，回归 skipped |
| 产品承载边界被误解为需要 H5 资产页 | 跨端策略 | 🟡 | 低 | 延续“商城/赚钱 H5，其它默认 Native”策略 | 若后续产品调整，再独立立项 |

---

## 13. 测试策略

本期 skipped，无新增 Web 自动化测试。原因不是“暂时未写”，而是**本期没有任何 Web route、页面组件、api-client 或 schema 进入实现范围**，因此不存在需要为 booking assets 新增的 Web 测试面。

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 无新增 | 无 booking assets Web route / api-client / schema / 页面交互进入范围 | — | — |

---

## 14. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | Web 本期 skipped |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/architecture/overview.md` | 承载策略 | Web 侧当前重点为 H5 承载能力，个人资产不在其内 |
| `wiki/features/app-shell/index.md` | 应用壳与入口职责 | 菜单中的“我的预约 / 我的下载”属于移动端应用壳入口 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `web/CLAUDE.md` | Web 五层架构与修改范围约束 |
| `docs/specs/2026-07-30-prd-11-user-assets/spec.md` | 明确 Web 不在 PRD-11 范围内 |
| `docs/specs/2026-07-30-prd-11-user-assets/design.md` | 明确资产页不做成 H5 |
| `docs/specs/2026-07-28-prd-13-mall/design-web.md` | H5 承载型功能的 Web 设计参考 |
| `docs/specs/2026-07-29-prd-14-earn/design-web.md` | H5 承载型功能的 Web 设计参考 |
