# 产品信息

## 产品名称

ShortDrama（短剧）

## 产品简介

短剧内容平台，提供竖屏短剧的浏览、推荐与播放体验。

## 竞品

- 红果

## 技术标识

| 字段 | 说明 | 值 |
| ---- | ---- | -- |
| appId | 应用唯一标识，用于第三方 SDK 集成、统计等 | com.djs66256.short_drama |
| schema | 自定义 URL Scheme，用于外部唤起与 deeplink | djsdrama:// |

## 页面承载策略

- 商城（mall）与赚钱（earn）频道使用 H5 页面承载，由 Native 容器接入。
- 除商城与赚钱外，其他业务页面当前只要求实现 Native 页面（iOS / Android），不作为独立 H5 交付范围。
