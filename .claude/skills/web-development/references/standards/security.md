# 安全 — Web

> 本文档定义 Web 端的安全规范。

---

## 1. XSS 防护

<!-- TODO: 补充 XSS 防护规范 -->

### 1.1 输出编码

<!-- TODO: React JSX 自动转义、dangerouslySetInnerHTML 禁用原则 -->

### 1.2 CSP

<!-- TODO: Content-Security-Policy Header 配置 -->

### 1.3 Trusted Types

<!-- TODO: Trusted Types API -->

---

## 2. CSRF 防护

<!-- TODO: 补充 CSRF 防护 -->

### 2.1 Token 方案

<!-- TODO: SameSite Cookie、CSRF Token -->

### 2.2 Server Actions

<!-- TODO: Next.js 内置 CSRF 保护 -->

---

## 3. 认证与授权

<!-- TODO: 补充认证方案 -->

### 3.1 Session 管理

<!-- TODO: httpOnly Cookie、Secure、SameSite -->

### 3.2 Token 存储

<!-- TODO: Access Token / Refresh Token 存储策略 -->

### 3.3 路由守卫

<!-- TODO: middleware.ts 鉴权、redirect -->

---

## 4. 敏感信息管理

<!-- TODO: 补充环境变量管理 -->

### 4.1 NEXT_PUBLIC_ 前缀

<!-- TODO: 客户端可访问 vs 仅服务端 -->

### 4.2 禁止硬编码

<!-- TODO: API Key、Secret 管理 -->
