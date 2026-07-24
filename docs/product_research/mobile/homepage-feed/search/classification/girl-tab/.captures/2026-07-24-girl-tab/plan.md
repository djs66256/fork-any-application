# 采集规划：红果 — 分类页女生 Tab

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/search/classification/girl-tab |
| 采集范围 | 从搜索分类页进入顶部“女生”内容池，记录女生 Tab 首屏结构与返回链路 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入分类页

- **操作**：从首页进入搜索页，再点击“分类”快捷入口
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 926 264 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-01-classification-page.png`
- **观察要点**：确认已处于分类页且顶部“女生”Tab 可见

*采集阶段回填：*
- **观察**：从首页进入分类页后，顶部可见“全部 / 男生 / 女生”三个大类 Tab，默认停留在“全部”；左侧为“时代背景 / 主题情节 / 角色设定”维度导航，右侧为标签矩阵，因此“女生”切换属于分类中心顶部一级内容池切换。
- **截图文件**：`assets/2026-07-24-step-01-classification-page.png`

### 步骤 2：点击“女生”Tab

- **操作**：在分类页点击顶部“女生”Tab，等待内容池更新
- **命令**：`adb shell input tap 466 126 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-02-girl-tab.png`
- **观察要点**：确认女生内容池下左侧维度与右侧标签矩阵的变化

*采集阶段回填：*
- **观察**：点击“女生”后，顶部 Tab 选中态切换到“女生”。左侧维度仍保留“时代背景 / 主题情节 / 角色设定”，但右侧标签矩阵被替换为更偏女性向的题材与人设，例如“职场 / 民国 / 校园 / 古装 / 女性成长 / 闪婚 / 暗恋成真 / 古风言情 / 现代言情 / 豪门恩怨 / 真假千金 / 萌宝 / 王妃 / 女帝 / 皇后 / 团宠”等，同时保留少量通用题材如“逆袭 / 重生 / 穿越 / 系统 / 悬疑推理”。
- **截图文件**：`assets/2026-07-24-step-02-girl-tab.png`

### 步骤 3：返回分类页/搜索页上文

- **操作**：从女生 Tab 状态执行返回，确认回到上一层
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-03-back-context.png`
- **观察要点**：记录返回落点和分类页状态是否保留

*采集阶段回填：*
- **观察**：从女生 Tab 返回后，直接回到搜索承接页，而不是停留在分类页“全部”态。这说明顶部“女生”Tab 所在分类页整体被视为搜索体系中的独立承接层，返回操作会退出整个分类中心。
- **截图文件**：`assets/2026-07-24-step-03-back-context.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-classification-page.png` | 截图 | 步骤 1 | 分类页入口态 |
| `assets/2026-07-24-step-02-girl-tab.png` | 截图 | 步骤 2 | 女生 Tab 首屏 |
| `assets/2026-07-24-step-02-girl-tab.xml` | XML | 步骤 2 | 女生 Tab 结构化界面树 |
| `assets/2026-07-24-step-03-back-context.png` | 截图 | 步骤 3 | 返回后的搜索页 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 14:16:00 | 进入分类页 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 926 264 && sleep 2` | 成功，进入分类页默认“全部”态 |
| 14:16:06 | 切换女生 Tab | `adb shell input tap 466 126 && sleep 2` | 成功，进入女生内容池 |
| 14:16:11 | 返回搜索页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 成功，直接返回搜索承接页 |

## 异常记录

无异常

## 执行状态

- [x] 步骤 1：进入分类页
- [x] 步骤 2：点击“女生”Tab
- [x] 步骤 3：返回分类页/搜索页上文
