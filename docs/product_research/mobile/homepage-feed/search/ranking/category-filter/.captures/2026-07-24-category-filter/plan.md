# 采集规划：红果 — 排行分类筛选入口

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/search/ranking/category-filter |
| 采集范围 | 打开排行页二级标签“分类”筛选入口，记录筛选面板结构、筛选维度与返回落点 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：打开排行页“分类”筛选面板

- **操作**：从首页进入搜索页，点击“排行”，在榜单页点击二级标签“分类”
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 322 264 && sleep 2 && adb shell input tap 972 556 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-01-category-filter.png`
- **观察要点**：确认“分类”入口是否进入独立页面或弹层，记录筛选维度与默认高亮项

*采集阶段回填：*
- **观察**：点击排行页二级标签“分类”后，页面并未进入新的榜单列表，而是弹出标题为“筛选”的覆盖式筛选面板。面板首屏包含“综合”分组，默认高亮“总榜”，并提供“女频”“男频”切换；下方继续按“时代背景”“主题情节”展示大量标签，如乡村、职场、民国、打脸虐渣、女性成长、系统等，说明该入口承接的是排行页的条件筛选能力，而不是独立榜单页面。
- **截图文件**：`assets/2026-07-24-step-01-category-filter.png`

### 步骤 2：关闭筛选面板并确认返回落点

- **操作**：从筛选面板执行返回，确认关闭后落点
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-02-back-context.png`
- **观察要点**：确认返回后是回到排行页还是直接回搜索页

*采集阶段回填：*
- **观察**：从筛选面板返回后，页面回到《红果热播榜》列表页，顶部仍显示“全部”与“热播榜”所在的榜单框架，没有直接回退到搜索页。说明“分类”是排行页内部弹层能力，返回动作只是关闭筛选面板并恢复原榜单上下文。
- **截图文件**：`assets/2026-07-24-step-02-back-context.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-category-filter.png` | 截图 | 步骤 1 | 排行页分类筛选面板 |
| `assets/2026-07-24-step-01-category-filter.xml` | XML | 步骤 1 | 排行页分类筛选面板界面树 |
| `assets/2026-07-24-step-02-back-context.png` | 截图 | 步骤 2 | 关闭筛选后的排行页上下文 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 15:35:48 | 打开排行页分类筛选面板 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 322 264 && sleep 2 && adb shell input tap 972 556 && sleep 2` | 成功，出现“筛选”面板，默认高亮“总榜”，并展示时代背景与主题情节标签矩阵 |
| 15:35:48 | 关闭筛选面板并返回榜单上下文 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 成功，返回《红果热播榜》列表页，而非搜索页 |

## 异常记录

- 暂无。

## 执行状态

- [x] 步骤 1：打开排行页“分类”筛选面板
- [x] 步骤 2：关闭筛选面板并确认返回落点
