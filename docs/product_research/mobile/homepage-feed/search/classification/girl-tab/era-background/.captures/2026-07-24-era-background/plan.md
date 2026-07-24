# 采集规划：红果 — 女生 Tab 时代背景标签组

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/search/classification/girl-tab/era-background |
| 采集范围 | 分类页女生 Tab 下“时代背景”标签组的首屏结构、标签内容与返回定位 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：记录女生 Tab 下的时代背景标签组

- **操作**：进入分类页“女生”Tab，保持左侧“时代背景”高亮，记录首屏标签
- **命令**：复用 `homepage-feed/search/classification/girl-tab/.captures/2026-07-24-girl-tab` 中已验证的女生 Tab 样本
- **截图**：保存为 `assets/2026-07-24-step-01-era-background.png`
- **观察要点**：确认该路径是否为同页分组、记录时代背景可见标签与页面结构

*采集阶段回填：*
- **观察**：女生 Tab 默认即停留在左侧“时代背景”分组。页面不是新的独立详情页，而是分类页女生内容池中的同页长页区域：左侧高亮“时代背景”，右侧顶部先展示时代背景标签“职场 / 民国 / 校园 / 古装”，下方继续顺延出现“主题情节”“角色设定”分组。说明 era-background 是女生 Tab 内部的默认可见标签组，而非单独跳转页面。
- **截图文件**：`assets/2026-07-24-step-01-era-background.png`

### 步骤 2：确认返回落点

- **操作**：复用女生 Tab 路径中已验证的返回样本，确认该分组所在层级的返回落点
- **命令**：复用 `homepage-feed/search/classification/girl-tab/.captures/2026-07-24-girl-tab/assets/2026-07-24-step-03-back-context.png`
- **截图**：保存为 `assets/2026-07-24-step-02-back-context.png`
- **观察要点**：确认返回是退出整个分类承接层，还是仅回到女生 Tab 其他分组

*采集阶段回填：*
- **观察**：从女生 Tab 所在分类承接层返回后，页面直接回到搜索承接页，而不是停留在分类页内部其他维度。这说明 era-background 作为女生 Tab 默认分组，仍属于搜索分类承接层内部状态。
- **截图文件**：`assets/2026-07-24-step-02-back-context.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-era-background.png` | 截图 | 步骤 1 | 女生 Tab 的时代背景标签组 |
| `assets/2026-07-24-step-01-era-background.xml` | XML | 步骤 1 | 女生 Tab 的时代背景标签组界面树 |
| `assets/2026-07-24-step-02-back-context.png` | 截图 | 步骤 2 | 返回后的搜索页 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 15:54:00 | 归档女生 Tab 时代背景样本 | `cp homepage-feed/search/classification/girl-tab/.captures/2026-07-24-girl-tab/assets/2026-07-24-step-02-girl-tab.png homepage-feed/search/classification/girl-tab/era-background/.captures/2026-07-24-era-background/assets/2026-07-24-step-01-era-background.png && cp homepage-feed/search/classification/girl-tab/.captures/2026-07-24-girl-tab/assets/2026-07-24-step-02-girl-tab.xml homepage-feed/search/classification/girl-tab/era-background/.captures/2026-07-24-era-background/assets/2026-07-24-step-01-era-background.xml` | 成功，归档女生 Tab 默认态中时代背景分组样本 |
| 15:54:00 | 归档返回样本 | `cp homepage-feed/search/classification/girl-tab/.captures/2026-07-24-girl-tab/assets/2026-07-24-step-03-back-context.png homepage-feed/search/classification/girl-tab/era-background/.captures/2026-07-24-era-background/assets/2026-07-24-step-02-back-context.png` | 成功，确认返回后直接回搜索承接页 |

## 异常记录

- 本轮未单独重新采集 era-background，因为父路径 `girl-tab` 的已验证样本已完整覆盖该默认分组的可见状态；因此直接复用同屏截图与 XML 拆分模块级文档。

## 执行状态

- [x] 步骤 1：记录女生 Tab 下的时代背景标签组
- [x] 步骤 2：确认返回落点
