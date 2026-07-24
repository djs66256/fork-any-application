# 采集规划：红果 — 男生 Tab 时代背景标签组

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/search/classification/boy-tab/era-background |
| 采集范围 | 进入分类页男生 Tab，记录“时代背景”标签组的首屏结构与返回链路 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入男生 Tab 的时代背景标签组

- **操作**：从首页进入搜索页，点击“分类”，再切换到顶部“男生”Tab
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 926 264 && sleep 2 && adb shell input tap 340 126 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-01-boy-tab.png`
- **观察要点**：确认左侧默认停留在“时代背景”，并记录右侧对应标签组

*采集阶段回填：*
- **观察**：进入男生 Tab 后，左侧默认高亮“时代背景”，右侧首个标签组即“时代背景”。可见标签包括“乡村 / 职场 / 民国 / 校园 / 历史古代 / 古装”，说明男生内容池会先用时空与生活场景对题材进行第一层归类；相较女生 Tab，多出了“乡村 / 历史古代”等更偏男性向成长与叙事空间的背景标签。
- **截图文件**：`assets/2026-07-24-step-01-boy-tab.png`

### 步骤 2：返回搜索页上文

- **操作**：从男生 Tab 状态执行返回，确认回到上一层
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-02-back-context.png`
- **观察要点**：记录返回落点，确认该标签组仍属于分类承接层内部状态

*采集阶段回填：*
- **观察**：从男生 Tab 浏览态返回后，直接回到搜索承接页，而不是停留在分类页。这说明“时代背景”标签组不是独立页面，而是男生内容池中的默认首屏分区。
- **截图文件**：`assets/2026-07-24-step-02-back-context.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-boy-tab.png` | 截图 | 步骤 1 | 男生 Tab 默认时代背景标签组 |
| `assets/2026-07-24-step-01-boy-tab.xml` | XML | 步骤 1 | 男生 Tab 界面树 |
| `assets/2026-07-24-step-02-back-context.png` | 截图 | 步骤 2 | 返回后的搜索页 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 14:33:00 | 进入男生 Tab | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 926 264 && sleep 2 && adb shell input tap 340 126 && sleep 2` | 成功，进入男生 Tab 默认“时代背景”分组 |
| 14:33:08 | 返回搜索页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 成功，直接返回搜索承接页 |

## 异常记录

无异常

## 执行状态

- [x] 步骤 1：进入男生 Tab 的时代背景标签组
- [x] 步骤 2：返回搜索页上文
