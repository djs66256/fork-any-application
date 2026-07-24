# 产品信息定义流程

产品信息是项目的基础元数据，必须在所有开发工作启动前确定。统一维护在 `PRODUCT.md` 中。

## 收集信息

向用户收集以下信息：

1. **产品名称**：应用的中文名 + 英文标识（如 ShortDrama（短剧））
2. **产品简介**：一句话描述产品是做什么的（如"竖屏短剧的浏览、推荐与播放平台"）
3. **竞品名称**：要 fork 的竞品应用名称（如 红果）
4. **应用标识**：
   - `appId`（Android package name / iOS bundle ID）：如 `com.example.app`
   - `schema`（URL Scheme）：如 `example://`

## PRODUCT.md 格式

```markdown
# 产品信息

## 产品名称

<英文名>（<中文名>）

## 产品简介

<一句话描述>

## 竞品

- <竞品名>

## 技术标识

| 字段 | 说明 | 值 |
| ---- | ---- | -- |
| appId | 应用唯一标识，用于第三方 SDK 集成、统计等 | <appId> |
| schema | 自定义 URL Scheme，用于外部唤起与 deeplink | <schema>:// |
```

## 约束

- **禁止在 skill、subagent、CLAUDE.md 等元内容文件中硬编码产品信息**，这些文件应引用 `PRODUCT.md`
- PRODUCT.md 只包含产品身份信息，不包含功能列表、技术架构等内容
- 产品名称和竞品名确定后不再频繁变更
