# 测试规范 — iOS

> 本文档定义 iOS 端的测试策略、框架选型与编写规范。

---

## 1. 测试金字塔

<!-- TODO: 补充测试分层策略 -->

| 层级 | 框架 | 占比 | 目标 |
|------|------|------|------|
| Unit | XCTest / Swift Testing | ~70% | 业务逻辑验证 |
| Integration | XCTest | ~20% | 组件集成 |
| UI | XCUITest | ~10% | 关键路径 E2E |

---

## 2. 单元测试

### 2.1 XCTest

<!-- TODO: 测试类命名、测试方法命名、setUp/tearDown -->

### 2.2 Swift Testing (Swift 6+)

<!-- TODO: @Test、#expect、参数化测试 -->

### 2.3 Mock 策略

<!-- TODO: Protocol-based mocking、Sourcery 自动生成 -->

### 2.4 ViewModel 测试

<!-- TODO: @Observable / @StateObject ViewModel 测试 -->

---

## 3. UI 测试

### 3.1 XCUITest

<!-- TODO: XCUIApplication、XCUIElement、断言 -->

### 3.2 ViewInspector

<!-- TODO: SwiftUI View 单元级测试 -->

---

## 4. 快照测试

### 4.1 SwiftSnapshotTesting

<!-- TODO: 快照生成、比对、更新策略 -->

### 4.2 快照存储

<!-- TODO: 参考图片存放路径、设备/OS 差异处理 -->

---

## 5. 测试覆盖率

<!-- TODO: 补充覆盖率目标 -->

### 5.1 工具

<!-- TODO: Xcode Code Coverage、xcresult 解析 -->

### 5.2 最低覆盖率

<!-- TODO: 行覆盖率、分支覆盖率目标 -->
