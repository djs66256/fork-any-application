# PRD-14 赚钱中心 Backend 代码评审

## 实现范围

本次 Backend 实现严格收敛在 PRD-14 赚钱中心最小闭环，完成了以下内容：

- shared schema：新增赚钱中心任务、奖励、overview、complete-task 请求/响应 schema 与类型
- repository interface：新增 `EarnRepositoryInterface`
- mock repository：新增 `EarnMockRepository`，提供受控 seed 数据、登录/匿名视角、任务完成幂等语义
- service：新增 `EarnService`，统一做 schema 校验与 `AppError` 透传 / 异常映射
- route：新增
  - `GET /api/earn/overview`
  - `POST /api/earn/complete-task`
- tests：补齐 schema / repository / service / route 测试
- registry：完成 earn repository 注册与测试替换入口接线

同时保持以下约束不变：

- 严格遵循 Route → Service → Repository → Shared/Infrastructure 分层
- 错误处理统一走 `withErrorHandler` + `AppError`
- `POST /api/earn/complete-task` 继续保持 Bearer-only
- 不新增依赖，不改 player/auth 既有 contract
- `GET /api/earn/overview` 对失效 Bearer token 采用单一稳定策略：降级为匿名视角并返回 200

## 关键实现结论

### 1. overview 鉴权策略

已按 design / plan 选择单一稳定策略：

- 无 token：返回匿名视角
- 合法 Bearer：返回登录视角
- 失效 Bearer：降级为匿名视角，不阻断首屏

### 2. complete-task 鉴权与幂等

- route 使用 `resolveRequiredAuthContext(request)`，保持 Bearer-only
- 相同用户重复完成同一代表性任务时，第二次返回 200 幂等成功结果，但 `coins_earned = 0`，不会重复加币
- 非代表性任务调用 complete-task 统一返回 `409 CONFLICT`
- 不存在任务统一返回 `404 NOT_FOUND`

### 3. mock 数据策略

- 匿名态 `coins = 0`
- 登录态返回受控基础金币 + 已完成代表性任务累计金币
- 连续看剧福利固定 7 项，确保前端可稳定渲染

## 验证结果

> 说明：首次运行定向测试时，当前 worktree 未安装 backend 依赖，出现 `vitest: command not found`。已先执行 `cd backend && npm install`，随后重新执行全部验证。

### 定向测试

- `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts`：通过
- `cd backend && npm run test -- src/repositories/__tests__/earn.mock.repository.test.ts`：通过
- `cd backend && npm run test -- src/services/earn/earn.service.test.ts`：通过
- `cd backend && npm run test -- src/app/api/__tests__/earn-overview.test.ts`：通过
- `cd backend && npm run test -- src/app/api/__tests__/earn-complete-task.test.ts`：通过

### 全量验证

- `cd backend && npm run test`：通过
- `cd backend && npm run lint`：通过
- `cd backend && npm run build`：通过

补充说明：

- 全量测试中存在若干既有 error-path 测试故意输出 `Unhandled error` 日志，但命令最终退出码为 0，属于测试覆盖预期，不构成阻塞

## 发现并修复的问题

1. 验证环境缺少 backend 依赖
   - 现象：定向测试首次执行时报错 `vitest: command not found`
   - 处理：执行 `cd backend && npm install` 后重跑全部验证

2. lint 阶段发现无关告警
   - `backend/src/app/api/dramas/rankings/route.ts` 存在未使用 import
   - `backend/src/services/earn/earn.service.test.ts` 中存在未使用参数
   - 处理：删除未使用 import，并对测试桩参数显式 `void`，随后重新通过 lint / test / build

## 遗留风险

- 当前 earn 数据源为 in-memory mock repository，重启进程后任务完成状态不会持久化；这是本期设计内约束，不影响最小闭环演示，但后续接真实仓储时需要引入真实幂等存储
- 目前只打通一个代表性任务的真实完成闭环，其它任务仍为展示或占位反馈，符合本期范围定义

## 结论

本次 Backend 代码实现已满足 PRD-14 earn 最小闭环要求：

- contract、分层、错误处理、Bearer-only 约束均已落地
- overview 失效 token 策略已稳定收敛为“降级匿名”
- complete-task 幂等、任务不存在、非代表性任务冲突等核心行为均有自动化测试覆盖
- 定向测试、全量测试、lint、build 均通过

结论：可接受，可进入后续流程，并可标记 `coding-platforms.backend = completed`。
