# 架构设计 — Android

> 本文档定义 Android 端的整体架构设计规范。

---

## 1. 整体架构

<!-- TODO: 补充架构图与分层说明 -->

```
UI (Composable)
    ↓
ViewModel (StateFlow)
    ↓
UseCase / Domain
    ↓
Repository
    ↓
DataSource (Remote / Local)
```

<!-- TODO: 各层职责详细说明 -->

---

## 2. 导航架构

<!-- TODO: 补充 Compose Navigation 路由设计 -->

### 2.1 路由定义

<!-- TODO: 路由表、参数传递、类型安全 -->

### 2.2 Deep Link

<!-- TODO: Deep Link 方案、外部唤起 -->

---

## 3. 状态管理

<!-- TODO: 补充状态管理方案 -->

### 3.1 ViewModel

<!-- TODO: ViewModel 使用规范、SavedStateHandle -->

### 3.2 StateFlow / SharedFlow

<!-- TODO: Flow 选择原则、冷流 vs 热流 -->

### 3.3 UI State

<!-- TODO: UI State 数据类设计、单状态对象 vs 多状态流 -->

---

## 4. 依赖注入

<!-- TODO: 补充 DI 方案（Hilt / Koin）及配置规范 -->

### 4.1 Module 组织

<!-- TODO: Module 拆分原则 -->

### 4.2 Scope

<!-- TODO: Singleton、ViewModelScoped、ActivityScoped -->

---

## 5. 模块化策略

<!-- TODO: 补充模块拆分原则 -->

### 5.1 模块划分

<!-- TODO: :app、:core、:feature-* 模块职责 -->

### 5.2 模块间通信

<!-- TODO: 接口定义、依赖方向 -->

---

## 6. 错误处理

<!-- TODO: 补充错误分类与处理策略 -->

### 6.1 错误分类

<!-- TODO: 网络错误、业务错误、系统错误 -->

### 6.2 降级策略

<!-- TODO: 兜底数据、离线缓存 -->
