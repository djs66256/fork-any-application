# PRD-14 赚钱中心 Web 代码评审

## 实现范围

本次实现严格控制在 Web 侧，并完成了 PRD-14 /earn 首页首版所需能力：

1. `/earn` App Router 路由壳层与 Feature 委托
2. Earn domain schema、config 与 Core API 封装
3. `complete-task` 显式 Bearer header 透传
4. Earn bridge（`earn.requestLogin` / `earn.openTaskPlayer`）
5. Earn host sync，仅监听 `CustomEvent('earn.hostMessage')`
6. `useEarnPage` 页面状态机：overview 加载、匿名登录引导、任务播放承接、任务完成闭环、restoreContext 恢复
7. 赚钱首页组件树：收益头图、新手任务、7 宫格福利、现金任务列表、反馈提示、登录引导层
8. 对应单测、Hook 测试、组件测试与全量回归测试

此外，为通过仓库当前 lint 基线，本次同时修复了 `web/` 内已有的 React Hooks lint 报错与一个 admin 图片规则告警，不涉及需求扩展。

## 验证结果

### 定向测试

- `cd web && npm test -- src/lib/schemas.test.ts src/lib/config.test.ts src/lib/api-client.test.ts`
  - 结果：通过
- `cd web && npm test -- src/lib/earn/api.test.ts src/features/earn/bridge/earn-bridge.test.ts src/features/earn/bridge/earn-host-sync.test.ts`
  - 结果：通过
- `cd web && npm test -- src/features/earn/hooks/useEarnPage.test.ts`
  - 结果：通过
- `cd web && npm test -- src/features/earn/EarnPageScreen.test.tsx`
  - 结果：通过

### 全量验证

- `cd web && npm test`
  - 结果：通过，20 个测试文件、150 个测试全部通过
- `cd web && npm run lint`
  - 结果：通过
- `cd web && npm run build`
  - 结果：通过
  - 备注：Next.js 输出既有 `middleware -> proxy` deprecation warning，为仓库现存提示，不是本次 earn 实现新增失败项

## 发现并修复的问题

### 1. Earn 任务完成闭环存在闭包状态滞后风险

问题：在 `useEarnPage` 初版实现中，`earn.completeTask` 回调使用 effect 闭包中的旧 token / overview 快照，导致测试场景下完成任务后金币未更新。

修复：
- 引入 `latestOverviewRef`、`latestTokenRef`、`latestStateRef`
- 任务完成闭环改为读取最新内存态，避免 host message 到达时拿到过期状态
- 用 Hook 测试覆盖成功完成、token 缺失、401 重登录等路径

### 2. 仓库现有 web lint 基线不通过

问题：`web/src/features/admin/hooks/*` 与 `web/src/lib/theme.tsx` 存在 `react-hooks/set-state-in-effect` 报错，导致按要求执行 `npm run lint` 失败。

修复：
- 将 admin hooks 的首次拉取改为 effect 内异步调度，避免同步 setState
- 调整 `theme.tsx` 初始主题读取方式，改为 lazy initializer，移除 effect 内同步 setState
- 清理 `DramaList.tsx` 中图片规则告警，确保 lint 全量通过

### 3. Bearer header 与 host sync 协议需显式锁定

问题：如果只靠组件层或隐式调用路径，容易回退到不显式传 Authorization，或误混入 mall 的 `window.message` 旧协议。

修复：
- 在 `web/src/lib/earn/api.ts` 中显式要求 `completeEarnTask(taskId, accessToken)`
- 在 `web/src/features/earn/bridge/earn-host-sync.ts` 中只监听 `earn.hostMessage` CustomEvent
- 用单测锁定 Authorization header 与 transport 行为

## 遗留风险

1. Native 联调尚未在本次 Web 任务内完成
   - 当前通过单测锁定了 Web contract，但真实 `__EARN_NATIVE_BRIDGE__` 注入、登录返回时序、播放器完成回调时序仍需依赖 iOS / Android 联调验证
2. `GET /api/earn/overview` 与 `POST /api/earn/complete-task` 目前按 contract 集成
   - 若后端最终 seed 数据字段与设计不一致，Web 侧会按 schema 进入错误态而不是脏渲染；这是受控失败，但仍需跨端确认
3. 仓库存在 Next.js `middleware` 约定弃用提示
   - 不阻塞本次交付，但后续升级建议统一迁移到 `proxy`

## 结论

本次 Web 实现已完成 PRD-14 计划要求的核心范围：`/earn` 首页、earn schema/config、Core API 封装、bridge、host sync、Hook 状态机、组件与测试均已落地，并通过定向测试、全量测试、lint 与 build 验证。

代码评审结论：通过。

可标记 `coding-platforms.web = completed`。
