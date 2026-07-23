# 深链 (Deeplink)

> 最后更新：2026-07-22

## 功能概述

通过自定义 URL Scheme 实现外部唤起应用的能力。当前仅 iOS 端声明了 URL Scheme，实际路由处理逻辑尚未实现。

- **覆盖端**：iOS（声明），Android/Web 待规划
- **核心价值**：支持外部链接唤起应用，为分享、推送通知落地页等功能奠定基础

## 入口与路由

### iOS
- Info.plist 声明：`ios/ShortDrama/Resources/Info.plist:38-48`
- URL Scheme：`djsdrama://`
- 尚未实现 Scheme 的路由处理逻辑（如 `onOpenURL`）

## 核心逻辑

当前仅做了 URL Scheme 的声明注册，未实现：

1. Scheme 解析（从 `djsdrama://` URL 中解析路由路径和参数）
2. 路由分发（根据解析结果跳转到对应页面）
3. 通用链接（Universal Links / App Links）

## 多端实现

### iOS
- 配置：`ios/ShortDrama/Resources/Info.plist:38-48`
- CFBundleURLName：`com.djs66256.short_drama`
- CFBundleURLSchemes：`["djsdrama"]`
- 与 PRODUCT.md 中定义的 schema `djsdrama://` 一致

### Android
- 尚未声明 App Links 或 Deep Links（`AndroidManifest.xml` 中无 intent-filter 配置）

### Web
- 不涉及（Web 端使用标准 HTTPS URL 路由）

## 配置参考

| 端 | Scheme | 配置位置 | 状态 |
|----|--------|---------|------|
| iOS | `djsdrama://` | Info.plist → CFBundleURLTypes | 📅 已声明，路由逻辑未实现 |
| Android | 待定 | AndroidManifest.xml → intent-filter | 📅 未实现 |
| Web | N/A | 使用 HTTPS 路由 | — |

## 依赖关系

- 依赖导航/路由模块（当前 `📅 规划中`）
- iOS 依赖 SwiftUI 的 `onOpenURL` 修饰符处理 URL 回调

## 已知限制

- 仅 iOS 声明了 Scheme，Android 未声明
- 无路由解析和分发逻辑
- 未配置 Universal Links（iOS）/ App Links（Android）
- 无 deeplink 测试覆盖
