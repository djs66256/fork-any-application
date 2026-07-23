# 安全 — Backend

> 本文档定义 Backend 端的安全规范。

---

## 1. 输入校验

<!-- TODO: 补充输入校验规范 -->

### 1.1 请求参数校验

<!-- TODO: Zod schema 校验所有输入 -->

### 1.2 SQL 注入防护

<!-- TODO: 参数化查询、ORM 使用 -->

### 1.3 类型安全

<!-- TODO: TypeScript + Zod 双重保障 -->

---

## 2. API 安全

<!-- TODO: 补充 API 安全规范 -->

### 2.1 Rate Limiting

<!-- TODO: 限流方案（IP/User/API Key 维度）-->

### 2.2 CORS

<!-- TODO: 允许的 Origin、Methods、Headers -->

### 2.3 API Key 管理

<!-- TODO: API Key 生成、存储、轮换 -->

### 2.4 请求大小限制

<!-- TODO: Body Size 限制 -->

---

## 3. 敏感数据

<!-- TODO: 补充敏感数据管理 -->

### 3.1 环境变量

<!-- TODO: 密钥管理、不支持 NEXT_PUBLIC_ 前缀 -->

### 3.2 数据脱敏

<!-- TODO: 日志中脱敏（手机号、密码、Token）-->

### 3.3 加密存储

<!-- TODO: 敏感字段加密、Vault 方案 -->

---

## 4. 依赖安全

<!-- TODO: 补充依赖安全 -->

### 4.1 依赖审计

<!-- TODO: npm audit、定期检查 -->

### 4.2 漏洞扫描

<!-- TODO: Dependabot / Snyk 集成 -->

### 4.3 版本锁定

<!-- TODO: package-lock.json 提交 -->
