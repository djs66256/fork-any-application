# CI/CD — Backend

> 本文档定义 Backend 端的持续集成与持续交付规范。

---

## 1. CI 流水线

<!-- TODO: 补充 CI 配置 -->

### 1.1 触发条件

<!-- TODO: PR、Push、Tag -->

### 1.2 流水线阶段

<!-- TODO: Lint → TypeCheck → Test → Build -->

---

## 2. 自动测试

<!-- TODO: 补充自动测试配置 -->

### 2.1 单元/集成测试

<!-- TODO: npm run test -->

### 2.2 测试环境

<!-- TODO: Supabase local / 测试实例 -->

---

## 3. 数据库 Migration

### 3.1 自动化 Migration

<!-- TODO: CI 中验证 Migration -->

### 3.2 部署前检查

<!-- TODO: Migration 兼容性检查 -->

### 3.3 回滚策略

<!-- TODO: Migration 回滚方案 -->

---

## 4. 自动部署

<!-- TODO: 补充部署方案 -->

### 4.1 Preview Deploy

<!-- TODO: PR 自动部署预览环境 -->

### 4.2 Production Deploy

<!-- TODO: main 分支自动部署 -->

### 4.3 环境变量

<!-- TODO: 各环境变量注入 -->

### 4.4 健康检查

<!-- TODO: 部署后健康检查 -->
