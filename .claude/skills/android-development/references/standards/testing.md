# 测试规范 — Android

> 本文档定义 Android 端的测试策略、框架选型与编写规范。

---

## 1. 测试金字塔

<!-- TODO: 补充测试分层策略 -->

| 层级 | 框架 | 占比 | 目标 |
|------|------|------|------|
| Unit | JUnit 5 + MockK | ~70% | 业务逻辑验证 |
| Integration | Robolectric | ~20% | Android 环境集成 |
| UI | Compose UI Test / Espresso | ~10% | 关键路径 E2E |

---

## 2. 单元测试

### 2.1 JUnit 5

<!-- TODO: 测试类命名、测试方法命名、断言风格 -->

### 2.2 MockK

<!-- TODO: mock、verify、slot、relaxed mock -->

### 2.3 Turbine

<!-- TODO: StateFlow/SharedFlow 测试、awaitItem -->

### 2.4 ViewModel 测试

<!-- TODO: ViewModel 单元测试模式 -->

### 2.5 Repository 测试

<!-- TODO: Repository 层测试策略 -->

---

## 3. UI 测试

### 3.1 Compose UI Test

<!-- TODO: ComposeTestRule、find、assert、perform -->

### 3.2 Espresso

<!-- TODO: 传统 View 体系测试 -->

---

## 4. 快照测试

### 4.1 Paparazzi

<!-- TODO: 无设备 Compose 渲染截图 -->

### 4.2 快照存储与比对

<!-- TODO: 黄金文件管理 -->

---

## 5. 测试覆盖率

<!-- TODO: 补充覆盖率目标 -->

### 5.1 工具

<!-- TODO: JaCoCo / Kover 配置 -->

### 5.2 最低覆盖率

<!-- TODO: 行覆盖率、分支覆盖率目标 -->
