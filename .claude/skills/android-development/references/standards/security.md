# 安全 — Android

> 本文档定义 Android 端的安全规范。

---

## 1. 数据安全

<!-- TODO: 补充数据安全规范 -->

### 1.1 敏感数据存储

<!-- TODO: EncryptedSharedPreferences、MasterKeys -->

### 1.2 加密方案

<!-- TODO: AES、RSA 使用场景、密钥管理 -->

### 1.3 剪贴板

<!-- TODO: 敏感内容禁止复制/自动清空 -->

---

## 2. 网络安全

### 2.1 SSL Pinning

<!-- TODO: 证书绑定方案 -->

### 2.2 证书校验

<!-- TODO: network_security_config.xml 配置 -->

### 2.3 明文传输

<!-- TODO: 禁止 HTTP 明文、"usesCleartextTraffic" -->

---

## 3. 代码安全

### 3.1 混淆

<!-- TODO: ProGuard / R8 规则 -->

### 3.2 反编译防护

<!-- TODO: 混淆等级、字符串加密 -->

### 3.3 敏感字符串

<!-- TODO: API Key、Secret 不应硬编码，使用 BuildConfig 或环境变量 -->

---

## 4. 运行时安全

### 4.1 Root 检测

<!-- TODO: Root 检测策略 -->

### 4.2 模拟器检测

<!-- TODO: 模拟器检测（根据业务需要）-->

### 4.3 截屏防护

<!-- TODO: FLAG_SECURE -->
