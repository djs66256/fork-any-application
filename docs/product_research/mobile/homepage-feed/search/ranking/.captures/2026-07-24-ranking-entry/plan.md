# 采集规划：红果 — 搜索页排行入口

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/search/ranking |
| 采集范围 | 从首页进入搜索承接页后，点击“排行”快捷入口，记录排行模块首屏与返回链路 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：从首页进入搜索承接页

- **操作**：冷启动红果，从首页点击右上搜索按钮
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-01-search-entry.png`
- **观察要点**：确认进入独立搜索页，且“排行”快捷入口可见

*采集阶段回填：*
- **观察**：从首页点击右上搜索后，进入独立搜索承接页。顶部为返回、搜索框和“搜索”按钮，下方一排可见“识剧 / 排行 / 上新 / 演员 / 分类”快捷入口；中部显示搜索历史与猜你想搜，为进入排行模块提供明确前置入口。
- **截图文件**：`assets/2026-07-24-step-01-search-entry.png`

### 步骤 2：点击“排行”快捷入口

- **操作**：在搜索页点击“排行”入口，等待排行模块稳定
- **命令**：`adb shell input tap 322 264 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-02-ranking-entry.png`
- **观察要点**：确认排行模块的页面结构、榜单分类和内容组织方式

*采集阶段回填：*
- **观察**：点击“排行”后跳转到独立榜单页《红果热播榜》。顶部有更新时间说明；中部先按内容类型分组（全部 / 真人剧 / 漫剧 / AI剧 / 演员），再按榜单逻辑分组（推荐榜 / 热播榜 / 臻果榜 / 预约榜 / 分类）；列表区按排名展示海报、剧名、标签与热度值，说明排行页是搜索体系中的深度分发层。
- **截图文件**：`assets/2026-07-24-step-02-ranking-entry.png`

### 步骤 3：返回搜索页

- **操作**：从排行模块返回，确认回到搜索承接页
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-03-back-search.png`
- **观察要点**：记录返回落点和搜索页状态是否保留

*采集阶段回填：*
- **观察**：从榜单页返回后，重新回到搜索承接页，搜索历史、猜你想搜和热搜榜模块仍然存在，说明返回后保留了搜索页原有状态，没有回到首页或重置搜索上下文。
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
| `assets/2026-07-24-step-02-ranking-entry.png` | 截图 | 步骤 2 | 排行模块首屏 |
| `assets/2026-07-24-step-03-back-search.png` | 截图 | 步骤 3 | 返回后的搜索页 |

## 采集日志

> 采集阶段逐条记录。

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 13:27:00 | 进入搜索承接页 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2` | 成功，进入搜索页并看到“排行”快捷入口 |
| 13:27:06 | 打开排行模块 | `adb shell input tap 322 264 && sleep 2` | 成功，进入《红果热播榜》 |
| 13:27:11 | 返回搜索页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 成功，返回搜索页且状态保留 |

## 异常记录

> 采集阶段回填。如有操作失败、页面异常、定位偏差等在此记录。

无异常

## 执行状态

- [x] 步骤 1：从首页进入搜索承接页
- [x] 步骤 2：点击“排行”快捷入口
- [x] 步骤 3：返回搜索页
