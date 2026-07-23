# 测试规范 — Web

> 本文档定义 Web 端的测试策略、框架选型与编写规范。

---

## 1. 测试金字塔

<!-- TODO: 补充测试分层策略 -->

| 层级 | 框架 | 占比 | 目标 |
|------|------|------|------|
| Unit | Vitest | ~70% | 工具函数、Hooks 逻辑 |
| Component | React Testing Library | ~20% | 组件行为验证 |
| E2E | Playwright | ~10% | 关键用户路径 |

---

## 2. 单元测试

### 2.1 Vitest

<!-- TODO: describe/it/expect、beforeEach/afterEach -->

### 2.2 纯函数测试

<!-- TODO: 工具函数、数据转换、Zod schema -->

### 2.3 Hook 测试

<!-- TODO: renderHook、act、waitFor -->

---

## 3. 组件测试

### 3.1 React Testing Library

<!-- TODO: render、screen、userEvent -->

### 3.2 查询策略

<!-- TODO: getByRole 优先、getByLabelText、getByText -->

### 3.3 异步组件

<!-- TODO: findBy、waitFor、act -->

### 3.4 Provider 包裹

<!-- TODO: QueryClient、Router、State 注入 -->

---

## 4. E2E 测试

### 4.1 Playwright

<!-- TODO: test、page、expect、fixtures -->

### 4.2 测试场景

<!-- TODO: 关键路径定义（注册、登录、核心业务流程）-->

### 4.3 CI 集成

<!-- TODO: Playwright CI 配置、截图/视频保留 -->

---

## 5. 视觉回归测试

<!-- TODO: 补充视觉回归方案 -->

### 5.1 Playwright Screenshot

<!-- TODO: toHaveScreenshot -->

### 5.2 Storybook + Chromatic

<!-- TODO: 组件级视觉回归 -->

---

## 6. 测试覆盖率

<!-- TODO: 补充覆盖率目标 -->

### 6.1 工具

<!-- TODO: Vitest coverage（v8 / istanbul）-->

### 6.2 最低覆盖率

<!-- TODO: 行覆盖率、分支覆盖率目标 -->
