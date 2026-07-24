# 采集规划：红果 — 搜索页分类入口

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/search/classification |
| 采集范围 | 从首页进入搜索承接页后，点击“分类”快捷入口，记录分类模块首屏与返回链路 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：从首页进入搜索承接页

- **操作**：冷启动红果，从首页点击右上搜索按钮
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-01-search-entry.png`
- **观察要点**：确认搜索页中“分类”快捷入口可见

*采集阶段回填：*
- **观察**：进入搜索承接页后，顶部可见返回、搜索框与“搜索”按钮，下方一排提供“识剧 / 排行 / 上新 / 演员 / 分类”五个快捷入口；中部仍显示搜索历史与猜你想搜，说明“分类”是搜索页首屏即暴露的一级发现入口。
- **截图文件**：`assets/2026-07-24-step-01-search-entry.png`

### 步骤 2：点击“分类”快捷入口

- **操作**：在搜索页点击“分类”入口，等待分类模块稳定
- **命令**：`adb shell input tap 926 264 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-02-classification-entry.png`
- **观察要点**：确认分类模块的页面结构、分类标签与内容组织方式

*采集阶段回填：*
- **观察**：点击“分类”后进入独立分类页。顶部有“全部 / 男生 / 女生”三个大类 Tab；左侧为纵向分类导航，当前选中“时代背景”，下方还有“主题情节”“角色设定”等分区；右侧是标签网格，按模块展示如“乡村 / 职场 / 民国 / 校园 / 历史古代 / 古装”等标签，说明分类页采用“左侧目录 + 右侧标签矩阵”的结构。
- **截图文件**：`assets/2026-07-24-step-02-classification-entry.png`

### 步骤 3：返回搜索页

- **操作**：从分类模块返回，确认回到搜索承接页
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-03-back-search.png`
- **观察要点**：记录返回落点和搜索页状态是否保留

*采集阶段回填：*
- **观察**：从分类页返回后，重新回到搜索承接页，搜索历史、猜你想搜与热搜榜模块继续保留，说明分类页是搜索体系下钻页，返回后不会重置搜索上下文。
- **截图文件**：`assets/2026-07-24-step-03-back-search.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

> 采集阶段执行完成后回填。

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-search-entry.png` | 截图 | 步骤 1 | 搜索承接页 |
| `assets/2026-07-24-step-02-classification-entry.png` | 截图 | 步骤 2 | 分类模块首屏 |
| `assets/2026-07-24-step-03-back-search.png` | 截图 | 步骤 3 | 返回后的搜索页 |

## 采集日志

> 采集阶段逐条记录。

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 13:42:00 | 进入搜索承接页 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2` | 成功，进入搜索页并看到“分类”快捷入口 |
| 13:42:06 | 打开分类模块 | `adb shell input tap 926 264 && sleep 2` | 成功，进入独立分类页 |
| 13:42:11 | 返回搜索页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 成功，返回搜索页且状态保留 |

## 异常记录

> 采集阶段回填。如有操作失败、页面异常、定位偏差等在此记录。

无异常

## 执行状态

- [x] 步骤 1：从首页进入搜索承接页
- [x] 步骤 2：点击“分类”快捷入口
- [x] 步骤 3：返回搜索页
