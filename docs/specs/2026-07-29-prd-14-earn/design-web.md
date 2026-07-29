# Web 端技术方案：PRD-14 赚钱中心

> 创建日期：2026-07-29
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

```text
app/earn/page.tsx (Server Route Shell)
→ features/earn/EarnPageScreen.tsx (Client Feature)
  → features/earn/components/EarnHeroCard
  → features/earn/components/EarnNewUserTaskCard
  → features/earn/components/EarnDailyRewardsGrid
  → features/earn/components/EarnCashTaskList
  → features/earn/components/EarnLoginPrompt
  → features/earn/components/EarnFeedbackToast
→ features/earn/hooks/useEarnPage.ts
→ lib/earn/api.ts / lib/api-client.ts
→ GET /api/earn/overview
→ POST /api/earn/complete-task

features/earn/bridge/earn-bridge.ts
→ Native bridge available ? requestLogin/openTaskPlayer
→ browser feedback fallback

features/earn/bridge/earn-host-sync.ts
→ subscribe `CustomEvent('earn.hostMessage')`
→ earn.syncAuthState / earn.restoreContext / earn.completeTask
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `web/src/app/earn/page.tsx` | 新增/修改 | 将不存在或占位的 earn 路由接成 route shell |
| `web/src/features/mall/` | 参考 | earn 延续 mall 的 feature-hook-bridge 模式，但命名空间与状态机独立 |
| `web/src/lib/api-client.ts` | 修改 | 在不破坏现有调用方的前提下支持按请求注入 Authorization header |
| `web/src/lib/schemas.ts` | 扩展 | 新增 earn overview / task / bridge / host sync schemas |
| `web/src/lib/config.ts` | 扩展 | 新增 earn route / bridgeEnabled / browser feedback 文案配置 |
| `web/src/lib/supabase.ts` | 参考 | 若浏览器内已有 Supabase session，可作为非 Native 调试场景的 Bearer 来源参考 |
| `web/src/features/earn/` | 新增 | 赚钱中心 H5 专属 feature 层 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `web/src/app/earn/page.tsx` | 新增/修改 | Route 层仅委托到 `EarnPageScreen` |
| `web/src/features/earn/EarnPageScreen.tsx` | 新增 | 赚钱中心首页主 Feature 组件 |
| `web/src/features/earn/components/*` | 新增 | 收益头图、新手任务、7 宫格、现金任务、登录引导、反馈等组件 |
| `web/src/features/earn/hooks/useEarnPage.ts` | 新增 | 首页状态机、overview/complete-task 调用、host sync 处理 |
| `web/src/features/earn/bridge/earn-bridge.ts` | 新增 | H5 → Native 登录/播放 bridge 适配层 |
| `web/src/features/earn/bridge/earn-host-sync.ts` | 新增 | Native → H5 auth/context/task 回传消息订阅 |
| `web/src/features/earn/config/earn-seed.ts` | 新增（可选） | 首页静态文案或默认反馈配置 |
| `web/src/lib/earn/api.ts` | 新增 | earn overview / complete-task API 封装 |
| `web/src/lib/schemas.ts` | 修改 | 新增 earn domain schema |
| `web/src/lib/config.ts` | 修改 | 新增 `config.earn` 配置块 |
| `web/src/features/earn/*.test.tsx` | 新增 | 覆盖状态机、bridge 降级、host sync、任务完成交互 |

---

## 3. 组件设计

### 3.1 组件层级树

```text
EarnPageScreen
├── EarnPageLayout
│   ├── EarnHeroCard
│   ├── EarnNewUserTaskCard
│   ├── EarnDailyRewardsGrid
│   │   └── EarnDailyRewardCell × 7
│   ├── EarnCashTaskSection
│   │   └── EarnTaskCard × N
│   ├── EarnLoginPromptOverlay (conditional)
│   ├── EarnFeedbackBanner / Toast (conditional)
│   └── EarnPageStateSection (loading / empty / error)
```

### 3.2 组件清单

| 组件名称 | 类型（Page / Section / Atom） | 职责 | Props 接口 |
|---------|------------------------------|------|-----------|
| `EarnPageScreen` | Page | 赚钱首页路由承接与状态装配 | — |
| `EarnHeroCard` | Section | 展示金币数、收益头图与登录入口 | `coins`, `isLoggedIn`, `onLoginClick` |
| `EarnNewUserTaskCard` | Section | 首屏新手任务展示与操作 | `task`, `onActionClick` |
| `EarnDailyRewardsGrid` | Section | 7 宫格连续看剧福利 | `rewards` |
| `EarnCashTaskSection` | Section | 现金任务列表 | `tasks`, `onTaskClick` |
| `EarnTaskCard` | Atom | 展示任务标题、奖励、描述、按钮 | `task`, `onClick` |
| `EarnLoginPromptOverlay` | Section | 匿名用户点击任务后的 H5 登录引导 | `visible`, `task`, `onContinue`, `onCancel` |
| `EarnFeedbackBanner` | Atom | 浏览器降级或任务完成结果反馈 | `message`, `onDismiss` |

### 3.3 组件接口定义

```typescript
interface EarnTaskCardProps {
  task: EarnTask;
  onClick: (task: EarnTask) => void;
}

interface EarnLoginPromptOverlayProps {
  visible: boolean;
  task: EarnTask | null;
  onContinue: () => void;
  onCancel: () => void;
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 父 → 子 | Props | overview 数据、登录态、任务按钮状态 |
| 子 → 父 | Callback Props | 任务点击、登录引导继续/取消、反馈关闭 |
| 跨层级 | Feature hook + 局部 reducer | 首页状态机、Native 回传事件、complete-task 结果 |
| 兄弟组件 | 提升状态到 `useEarnPage` | 收益头图、新手任务卡、任务列表共享 overview / auth / feedback |

### 3.5 响应式设计

| 断点 | 宽度范围 | 布局策略 | 关键变化 |
|------|---------|---------|---------|
| Mobile | `< 768px` | 单列赚钱首页布局 | 与 Native 容器目标布局一致 |
| Tablet | `768px - 1024px` | 居中容器 + 单列 | 放宽卡片宽度与间距 |
| Desktop | `> 1024px` | 仍保持移动稿居中容器 | 不扩展为桌面钱包站布局 |

### 3.6 无障碍（A11y）

| 关注点 | 策略 |
|--------|------|
| 语义化 HTML | 登录、立即领取、重试按钮使用 button 语义 |
| 键盘导航 | 登录引导 overlay 支持 Escape 关闭 |
| 屏幕阅读器 | 金币数、奖励金额、任务状态提供清晰 `aria-label` |
| 色彩对比度 | 状态色与按钮满足 WCAG AA |

---

## 4. 状态管理方案

### 4.1 方案选择

| 维度 | 选择 | 理由 |
|------|------|------|
| 全局状态 | 不新增全局 store | 赚钱状态主要局限在 `/earn` 路由内 |
| 服务端状态 | 自定义 hook + `api-client.ts` | 避免新增额外依赖 |
| 表单状态 | 无 | 本期没有表单输入 |
| 路由状态 | Next.js App Router | 与现有 Page 层约束一致 |
| URL 状态 | 首版不把任务/分页写入 query | 降低 Native 容器与浏览器模式差异 |

### 4.2 状态划分

| 状态名称 | 范围（全局/页面/局部） | 存储方式 | 跨页面持久化 |
|---------|----------------------|---------|------------|
| `overview` | 页面 | `useReducer` / `useState` | 否 |
| `isLoading` / `errorMessage` | 页面 | `useReducer` | 否 |
| `isLoggedIn` | 页面 | host sync + overview | 否 |
| `loginPromptVisible` | 页面 | `useReducer` | 否 |
| `activeTask` | 页面 | `useReducer` | 否 |
| `pendingRestoreReason` | 页面 | `useReducer` | 否 |
| `pendingCompletionTaskId` | 页面 | `useReducer` | 否 |
| `feedbackMessage` | 页面 | `useReducer` | 否 |
| `browserMode` | 页面初始化 | bridge 探测 | 否 |

### 4.3 全局状态结构

```typescript
interface EarnPageState {
  overview: EarnOverviewResponse | null;
  isLoading: boolean;
  errorMessage: string | null;
  isLoggedIn: boolean;
  apiAccessToken: string | null;
  loginPromptVisible: boolean;
  activeTask: EarnTask | null;
  pendingRestoreReason: EarnRestoreContext['reason'] | null;
  pendingCompletionTaskId: string | null;
  feedbackMessage: string | null;
  isCompletingTask: boolean;
}
```

### 4.4 服务端状态管理

```typescript
export async function fetchEarnOverview(): Promise<EarnOverviewResponse> {
  const response = await api.get('/api/earn/overview');
  return EarnOverviewResponseSchema.parse(response);
}

export async function completeEarnTask(
  input: CompleteEarnTaskRequest,
  authToken: string,
): Promise<CompleteEarnTaskResponse> {
  const payload = CompleteEarnTaskRequestSchema.parse(input);
  const response = await api.post('/api/earn/complete-task', payload, {
    headers: {
      Authorization: `Bearer ${authToken}`,
    },
  });
  return CompleteEarnTaskResponseSchema.parse(response);
}
```

### 4.5 状态流转

```text
进入 /earn
→ loadOverview
→ success(content) | empty | error

匿名点击代表性任务
→ showLoginPrompt
→ cancel => hide prompt
→ continue => bridge.requestLogin(context)

已登录点击代表性任务
→ bridge.openTaskPlayer(context)
→ waiting host message earn.completeTask
→ receive completed=true
→ POST /api/earn/complete-task
→ update coins/task state or reload overview
```

---

## 5. 路由设计

### 5.1 路由清单

| 路径 Pattern | 页面组件 | 参数 | 认证守卫 | 懒加载 | 说明 |
|-------------|---------|------|---------|--------|------|
| `/earn` | `EarnPageScreen` | — | 否 | 否 | 赚钱首页 H5 |

### 5.2 路由层级

```text
App Router
└── /earn
    └── EarnPageScreen
```

### 5.3 路由守卫

- `/earn` 不设置路由级认证守卫，匿名用户可浏览赚钱首页。
- 是否需要登录由任务点击逻辑在页内分流，不在 route 层 redirect。
- 浏览器模式直接访问 `/earn` 时保持页面可用，不跳到登录页或其它频道。

### 5.4 数据预取与加载

| 页面 | 预取方式 | 预取内容 |
|------|---------|---------|
| `/earn` | 客户端首次加载 | `GET /api/earn/overview` |

---

## 6. API 调用层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | `web/src/lib/api-client.ts` | 继续使用 fetch wrapper |
| 请求参数构造 | `web/src/lib/earn/api.ts` | earn API 专用封装 |
| 响应校验 | Zod schema | `EarnOverviewResponseSchema` / `CompleteEarnTaskResponseSchema` |
| bridge 适配 | `features/earn/bridge/earn-bridge.ts` | 封装 Native 登录与播放消息 |
| host sync | `features/earn/bridge/earn-host-sync.ts` | 订阅宿主回传消息 |

### 6.2 客户端封装

```typescript
export const earnApi = {
  async getOverview(): Promise<EarnOverviewResponse> {
    const result = await api.get('/api/earn/overview');
    return EarnOverviewResponseSchema.parse(result);
  },

  async completeTask(taskId: string): Promise<CompleteEarnTaskResponse> {
    const result = await api.post('/api/earn/complete-task', { task_id: taskId });
    return CompleteEarnTaskResponseSchema.parse(result);
  },
};
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 首屏 overview 失败 | 0（手动重试） | — | 页面“重试”按钮触发 |
| complete-task 失败 | 0（用户显式重试或重新触发） | — | 不自动重复提交，避免奖励重复语义混乱 |
| host message 丢失 | 0 | — | 通过 `restoreContext` 时重拉 overview 兜底 |

### 6.4 请求 Hook 封装

- `useEarnPage` 统一处理：
  - overview loading / empty / error；
  - 登录引导弹层；
  - `earn.syncAuthState` / `earn.restoreContext` / `earn.completeTask` 事件订阅；
  - complete-task 成功后的局部更新或重拉 overview。
- `earn-host-sync.ts` 负责把宿主消息映射为 Web 可消费的事件：
  - 只监听 `window` 上的 `earn.hostMessage` `CustomEvent`；
  - `earn.syncAuthState`：刷新 `isLoggedIn`，并把 `apiAccessToken` 写入 hook 内存态；
  - `earn.restoreContext`：关闭弹层并按需重拉 overview；
  - `earn.completeTask`：仅当 `completed=true` 且内存中存在 token 快照时触发 API 提交。
- 首版沿用现有 `api-client.ts` 对 `{ error: { code, message } }` 的解析能力。
- `complete-task` 收到 `401/AUTH_UNAUTHORIZED` 时，hook 必须：
  - 清空 `apiAccessToken`；
  - 回退 `isLoggedIn=false` 或等待下一次 `earn.syncAuthState` 覆盖；
  - 弹出登录引导，不伪造奖励成功。

---

## 7. SSR / CSR 策略

### 7.1 渲染策略选择

| 页面 | 渲染策略 | 原因 |
|------|---------|------|
| `/earn` | CSR-first（Route Shell 可为 Server Component） | 需要运行时 bridge 探测、登录引导、任务完成回调与反馈状态 |

### 7.2 框架支持

- Next.js App Router Page 层继续只做路由委托。
- 业务 UI 与状态逻辑全部下沉到 `src/features/earn/`。
- 不引入新的状态管理或数据请求框架。

### 7.3 数据预取策略

| 页面 | 预取方法 | 预取数据 |
|------|---------|---------|
| `/earn` | 客户端 `useEffect` / mount 触发 | overview |

### 7.4 SEO 策略

- 赚钱中心主要服务于 Native WebView 承载，不以 SEO 为目标。
- 保留基础 `title` / `description`，不引入结构化数据。

---

## 8. 性能优化

### 8.1 优化清单

| 优化项 | 策略 | 目标 |
|--------|------|------|
| 首屏加载 | 先渲染稳定骨架区块，再请求 overview | 快速看到页面结构 |
| 数据更新 | complete-task 成功后优先局部更新，必要时再重拉 overview | 降低重复请求 |
| 组件拆分 | 收益头图、任务卡、福利宫格拆分 | 降低重渲染范围 |
| bridge 降级 | 浏览器模式不跳空页，直接反馈 | 避免无效导航 |

### 8.2 加载体验

| 场景 | 策略 |
|------|------|
| 首屏加载 | 收益头图 / 模块 skeleton |
| overview 失败 | 首屏错误态 + 重试 |
| complete-task 中 | 按钮禁用或局部 loading |
| 登录返回 | 通过 host sync 轻量刷新，不整页闪烁 |

---

## 9. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| API Base URL | `NEXT_PUBLIC_API_URL` | 现有配置 | 现有配置 | 继续复用现有 API 基址 |
| 应用名 | `NEXT_PUBLIC_APP_NAME` | 现有配置 | 现有配置 | 复用现有 config |
| 应用版本 | `NEXT_PUBLIC_APP_VERSION` | 现有配置 | 现有配置 | 复用现有 config |
| Earn bridge 开关（可选） | `NEXT_PUBLIC_EARN_BRIDGE_ENABLED` | `true/false` | `true/false` | 本地开发显式控制宿主模式 |

> ⚠️ 禁止硬编码任何环境地址、接口前缀、returnTarget 或 bridge 对象名。所有环境与受控常量集中在 config / schema / bridge 模块中。

---

## 10. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `GET /api/earn/overview` | 首次进入 `/earn`、登录返回、容器重建后 | 固定无参请求 | 初始化/刷新 overview | 首屏错误态 + 重试 |
| `POST /api/earn/complete-task` | 收到 `earn.completeTask(completed=true)` | host payload 的 `taskId` | 更新金币与任务状态 | 内联反馈 + 可兜底重拉 overview |

---

## 11. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Web 端实现方式 |
|---------|---------------|---------------|
| H5 承载赚钱首页 | `/earn` 为唯一 H5 首页 | `app/earn/page.tsx` → `EarnPageScreen` |
| overview 首屏 contract | 进入首页即拉取 overview | `useEarnPage` mount 时请求 `getOverview()` |
| 登录拦截顺序 | 先页内引导，再请求 Native 登录 | `EarnLoginPromptOverlay` + `requestEarnLogin()` |
| 代表性任务播放承接 | 已登录点击任务后发 `earn.openTaskPlayer` | `earn-bridge.ts` 组装 `EarnTaskContext` |
| 任务完成闭环 | 仅响应 Native 发回的 `earn.completeTask` | `earn-host-sync.ts` 订阅后调用 `completeTask()` |
| 登录态同步 | 宿主提供权威登录态与 Bearer 快照 | `earn.syncAuthState` 更新 `isLoggedIn` 与内存态 `apiAccessToken` |
| 返回赚钱上下文 | 登录/任务返回后停留 `/earn` | 收到 `earn.restoreContext` 后关闭弹层并按需刷新 overview |
| 浏览器模式降级 | 无 Native bridge 时仅做反馈 | bridge 模块返回 `browser-fallback`，页面展示受控提示 |
| 独立命名空间 | 不复用 `mall.*` 消息 | earn 独立 schema / bridge / host sync 文件 |
| host sync transport | Native 统一注入 `CustomEvent('earn.hostMessage')` | `earn-host-sync.ts` 只解析该事件名与其 `detail` 负载 |

---

## 12. 边界与错误处理

### 12.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | `api-client.ts` + schema parse | 统一 HTTP / Network / Timeout 错误 |
| Feature 状态层 | `useEarnPage` reducer | 区分首屏错误、任务完成错误、浏览器反馈 |
| UI 层 | 内联错误区 + 反馈条 | 避免整页白屏 |
| bridge 层 | `earn-bridge.ts` fallback | bridge 缺失时停留当前页并展示说明 |

### 12.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `VALIDATION_ERROR` | 任务参数异常，请稍后重试 | 反馈条 / 首屏错误态 |
| `INVALID_PARAMS` | 任务参数异常，请稍后重试 | 反馈条 |
| `AUTH_UNAUTHORIZED` | 请先登录后再领取奖励 | 登录引导 / 反馈条 |
| `NOT_FOUND` | 任务不存在或已失效 | 反馈条 |
| `CONFLICT` | 任务已处理，请刷新后查看 | 反馈条 |
| `INTERNAL_ERROR` | 服务开小差了，请稍后重试 | 首屏错误态 / 反馈条 |
| `SERVICE_UNAVAILABLE` | 服务暂不可用，请稍后再试 | 首屏错误态 / 反馈条 |
| `NETWORK_ERROR` | 网络异常，请检查后重试 | 首屏错误态 / 反馈条 |

### 12.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 首屏 overview 失败 | `/api/earn/overview` 超时 / 5xx | 展示首屏错误态与重试 | 🔴 |
| bridge 不存在 | 浏览器 / 本地开发模式 | 展示“请在 App 内完成”或“暂时无法打开登录”反馈 | 🔴 |
| bridge 调用抛错 | Native 宿主未注册能力 | 展示轻提示，不跳空白页 | 🔴 |
| Native 未同步登录态 | 首次加载后未收到 `earn.syncAuthState` | 默认按 overview 返回值或匿名处理 | 🔴 |
| 未收到 Bearer 快照 | `isLoggedIn=true` 但 `apiAccessToken` 为空 | 不调用 `complete-task`，提示重新登录 | 🔴 |
| 收到 `earn.completeTask(completed=false)` | 播放未完成 | 只恢复上下文，不调用 API | 🔴 |
| complete-task 失败 | 接口 4xx/5xx | 不伪造到账结果，提示用户稍后重试 | 🔴 |
| complete-task 401 | token 失效或宿主快照过期 | 清空 token 快照，重新展示登录引导 | 🔴 |
| 登录返回 | `earn.restoreContext(reason='login-return')` | 关闭登录引导，必要时重拉 overview | 🔴 |
| 容器被重建 | `reason='container-recreated'` | 清理临时态并重拉 overview | 🟡 |
| 重复收到完成回调 | 宿主重复发相同 taskId | hook 内通过 pending state / 后端幂等避免重复加币 | 🟡 |

### 12.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `/earn` 首屏 | 模块 skeleton | 渲染收益与任务 | 展示空态说明 | 首屏错误态 + 重试 | — |
| 登录引导层 | — | 显示拦截文案 | — | bridge 失败反馈 | 非法 task 上下文时禁止继续 |
| complete-task 反馈 | 局部按钮 loading | 更新金币/任务状态 | — | 反馈条提示失败 | — |

---

## 13. 测试策略

### 13.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | earn schema、bridge adapter、reducer 状态流转 | 核心逻辑全覆盖 | Vitest |
| 组件测试 | 首屏渲染、登录引导、错误态、浏览器降级反馈 | 关键交互全覆盖 | Testing Library |
| 集成测试 | `/earn` 路由 + mock API + host message 行为 | 核心流程覆盖 | Vitest + MSW |

### 13.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| WEB-EARN-01 | 首页加载成功 | mock overview 成功 | 进入 `/earn` | 收益头图、新手任务、7 宫格、现金任务可见 | 组件 |
| WEB-EARN-02 | 首屏失败 | overview API 500 | 进入 `/earn` | 展示错误态与重试按钮 | 组件 |
| WEB-EARN-03 | 匿名点击任务 | `isLoggedIn=false` | 点击代表性任务 | 显示登录引导层 | 组件 |
| WEB-EARN-04 | 浏览器模式继续登录 | bridge 不可用 | 点击“继续登录” | 显示受控反馈，不跳页 | 单元 / 组件 |
| WEB-EARN-05 | 已登录点击代表性任务 | `isLoggedIn=true` | 点击任务 | 调用 `earn.openTaskPlayer` bridge | 单元 |
| WEB-EARN-06 | 收到登录成功同步 | host message `earn.syncAuthState` | 订阅回调触发 | `isLoggedIn` 更新为 true | 单元 |
| WEB-EARN-07 | 收到任务完成回调 | host message `earn.completeTask(completed=true)` | 触发回调 | 调用 `POST /api/earn/complete-task` 并更新 UI | 集成 |
| WEB-EARN-08 | 收到未完成回调 | host message `completed=false` | 触发回调 | 不调用 complete-task API | 单元 |
| WEB-EARN-09 | 容器重建恢复 | host message `earn.restoreContext(reason='container-recreated')` | 触发回调 | 重拉 overview | 集成 |

### 13.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| API 请求 | MSW | 模拟 overview / complete-task 成功与失败 |
| Native bridge | 手动挂载 `window.__EARN_NATIVE_BRIDGE__` stub | 验证登录 / 播放分流 |
| host sync | `window.postMessage` 或 `CustomEvent` stub | 验证回传消息处理 |
| 路由 | Next.js navigation mock | 本期 `/earn` 本身无复杂子路由 |

---

## 14. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 复用现有 Next.js / React / Zod / Vitest / Testing Library |

> ⚠️ 新增开源依赖前必须征得用户同意（遵守根目录 CLAUDE.md 开发约束）。

---

## 15. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 直接复用 mall bridge / host sync 导致 earn 消息语义混乱 | Web / iOS / Android | 🔴 | 中 | 独立新增 `earn-bridge.ts` 与 `earn-host-sync.ts` | 浏览器模式全部本地反馈 |
| host sync 通道与 Native 注入方式不一致 | Web / Native | 🔴 | 中 | earn 方案显式定义统一协议，并在 Web 侧做兼容解析 | 恢复时直接重拉 overview |
| complete-task 成功后 UI 与后端状态不一致 | Web / Backend | 🟡 | 中 | 成功后优先局部更新，必要时立即重拉 overview | 完整重拉 overview |
| 组件直接硬编码 `/earn` / `source=earn` | Web | 🟡 | 中 | 上下文拼装集中到 bridge/schema/config 模块 | 回退为单一常量文件 |

---

## 16. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/app-shell/index.md` | Web、已知限制 | earn 仍为占位，需要新增真实 H5 首页 |
| `wiki/architecture/overview.md` | 承载策略 | earn 与 mall 继续按 H5 承载 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `web/CLAUDE.md` | Web 五层架构、SSR-first、Core API 约束 |
| `web/src/lib/config.ts` | 当前只有 mall 配置，earn 需新增独立配置块 |
| `web/src/lib/api-client.ts` | 现有 fetch wrapper 与错误解析 |
| `web/src/features/mall/hooks/useMallPage.ts` | reducer + host sync + 登录拦截的最近模式 |
| `web/src/features/mall/bridge/mall-bridge.ts` | H5 → Native bridge 写法参考 |
| `web/src/features/mall/bridge/mall-host-sync.ts` | 当前只监听 `message` 的 host sync 参考 |
| `docs/specs/2026-07-28-prd-13-mall/design-web.md` | mall 平台设计范式 |
| `docs/specs/2026-07-29-prd-14-earn/design.md` | earn shared contract |
