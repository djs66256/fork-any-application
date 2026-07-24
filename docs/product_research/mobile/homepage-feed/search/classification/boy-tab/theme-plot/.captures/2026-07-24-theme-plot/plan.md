# 采集规划：红果 — 男生 Tab 主题情节标签组

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/search/classification/boy-tab/theme-plot |
| 采集范围 | 进入分类页男生 Tab，切换左侧“主题情节”，记录标签组结构与返回链路 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入男生 Tab 的主题情节标签组

- **操作**：从首页进入搜索页，点击“分类”，切换到顶部“男生”Tab，再点击左侧“主题情节”
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 926 264 && sleep 2 && adb shell input tap 340 126 && sleep 2 && adb shell input tap 118 408 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-01-theme-plot.png`
- **观察要点**：确认左侧“主题情节”高亮，并记录右侧主题情节标签矩阵与同页结构

*采集阶段回填：*
- **观察**：点击左侧“主题情节”后，页面仍停留在分类中心同一页，左侧以橙色高亮“主题情节”。右侧并不是只保留单一模块，而是继续以纵向长页方式展示“时代背景 / 主题情节 / 角色设定”三个分组，其中主题情节分组位于中部并暴露大量男频题材标签。首屏可见标签包括“逆袭 / 马甲 / 都市日常 / 重生 / 穿越 / 系统 / 亲情 / 奇幻脑洞 / 穿书 / 战神归来 / 异能 / 传承觉醒 / 玄幻仙侠 / 赘婿逆袭 / 娱乐圈 / 剧情 / 无敌神医 / 悬疑推理 / 喜剧”等。
- **截图文件**：`assets/2026-07-24-step-01-theme-plot.png`

### 步骤 2：返回搜索页上文

- **操作**：从主题情节浏览态执行返回，确认回到上一层
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-02-back-context.png`
- **观察要点**：记录返回落点，确认主题情节标签组仍属于分类承接层内部状态

*采集阶段回填：*
- **观察**：从男生 Tab 的主题情节浏览态返回后，直接回到搜索承接页，而不是停留在分类页。这说明主题情节仍然是分类中心内部的锚点分组，而非独立页面。
- **截图文件**：`assets/2026-07-24-step-02-back-context.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-theme-plot.png` | 截图 | 步骤 1 | 男生 Tab 主题情节标签组 |
| `assets/2026-07-24-step-01-theme-plot.xml` | XML | 步骤 1 | 男生 Tab 主题情节界面树 |
| `assets/2026-07-24-step-02-back-context.png` | 截图 | 步骤 2 | 返回后的搜索页 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 15:00:00 | 进入男生 Tab 主题情节 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 926 264 && sleep 2 && adb shell input tap 340 126 && sleep 2 && adb shell input tap 118 408 && sleep 2` | 成功，左侧“主题情节”高亮并显示主题情节标签组 |
| 15:00:10 | 返回搜索页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 成功，直接返回搜索承接页 |

## 异常记录

无异常

## 执行状态

- [x] 步骤 1：进入男生 Tab 的主题情节标签组
- [x] 步骤 2：返回搜索页上文
