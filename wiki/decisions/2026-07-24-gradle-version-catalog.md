# 2026-07-24 — Gradle Version Catalog 依赖管理

> 状态：已采纳
> 决策人：daniel
> 最后更新：2026-07-24

## 背景

Android 工程需要管理数十个依赖库（Compose、Hilt、Retrofit、OkHttp 等），传统方式在每个 `build.gradle.kts` 中直接声明版本号，容易产生版本漂移。

## 决策

使用 Gradle Version Catalog（`gradle/libs.versions.toml`）集中管理所有依赖版本。所有模块通过别名（alias）引用依赖，版本锁定在一处。

## 备选方案

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| Version Catalog (libs.versions.toml) | Gradle 官方推荐；集中管理；IDE 自动补全 | 需额外维护 toml 文件 | 采纳 |
| 直接在 build.gradle.kts 声明 | 简单直接 | 版本号分散；不一致风险高 | 拒绝 |

## 影响

- AGP 8.7.0 / Kotlin 2.0.21 / Compose BOM 2024.12.01 / Hilt 2.53.1 等版本统一锁定
- 所有模块引用依赖时使用 `implementation(libs.hilt.android)` 等 alias 方式
- 版本升级只需修改 `libs.versions.toml` 一处

### 源文件

- `android/gradle/libs.versions.toml` — Version Catalog
- `android/build.gradle.kts` — 根构建脚本
- `android/app/build.gradle.kts` — app 模块依赖引用

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-24 | 初始创建，记录 Gradle Version Catalog 选型决策 |

---

*本文档由 llm-wiki skill 自动维护。*
