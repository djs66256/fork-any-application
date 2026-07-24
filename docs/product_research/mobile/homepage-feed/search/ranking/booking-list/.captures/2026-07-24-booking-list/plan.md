# 采集规划：红果 — 排行页预约榜子榜单

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/search/ranking/booking-list |
| 采集范围 | 从排行页切换到“预约榜”子榜单，记录预约榜首屏结构与返回链路 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入排行页

- **操作**：从首页进入搜索页，再点击“排行”快捷入口
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 322 264 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-01-ranking-page.png`
- **观察要点**：确认已进入排行页，默认停留在热播榜

*采集阶段回填：*
- **观察**：从搜索页进入排行页后，默认展示《红果热播榜》。页面具备一级内容类型标签“全部 / 真人剧 / 漫剧 / AI剧 / 演员”和二级榜单标签“推荐榜 / 热播榜 / 臻果榜 / 预约榜 / 新剧榜 / 分类”，说明“预约榜”属于同一榜单框架中的子榜单切换。
- **截图文件**：`assets/2026-07-24-step-01-ranking-page.png`

### 步骤 2：切换到“预约榜”子榜单

- **操作**：在排行页点击二级标签“预约榜”
- **命令**：`adb shell input tap 698 555 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-02-booking-list.png`
- **观察要点**：确认标题、说明文案、条目指标与按钮是否切换为预约语义

*采集阶段回填：*
- **观察**：点击“预约榜”后，页面切换为《红果预约榜》，顶部说明改为“基于红果预约/播放等综合期待值排序”。列表右侧主指标由“热度”改为“期待”，条目底部显示“预告 · xx万人预约 · 预计xx月上线”，右侧按钮统一为“预约”，说明该榜单聚焦未上线或待播内容的预约转化与期待值运营。
- **截图文件**：`assets/2026-07-24-step-02-booking-list.png`

### 步骤 3：返回排行/搜索页上文

- **操作**：从预约榜状态执行返回，确认回到上一层
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-03-back-context.png`
- **观察要点**：记录返回落点和排行页状态是否保留

*采集阶段回填：*
- **观察**：从预约榜返回后，直接回到搜索承接页，而不是停留在热播榜或排行页默认态。这说明预约榜与其它子榜单一样，被视为排行体系内部状态，返回操作会退出整个排行承接层。
- **截图文件**：`assets/2026-07-24-step-03-back-context.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-ranking-page.png` | 截图 | 步骤 1 | 排行页热播榜入口态 |
| `assets/2026-07-24-step-02-booking-list.png` | 截图 | 步骤 2 | 预约榜子榜单首屏 |
| `assets/2026-07-24-step-03-back-context.png` | 截图 | 步骤 3 | 返回后的搜索页 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 14:12:00 | 进入排行页 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 322 264 && sleep 2` | 成功，进入排行页默认热播榜 |
| 14:12:06 | 切换预约榜 | `adb shell input tap 698 555 && sleep 2` | 成功，进入预约榜子榜单 |
| 14:12:11 | 返回搜索页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 成功，直接返回搜索承接页 |

## 异常记录

无异常

## 执行状态

- [x] 步骤 1：进入排行页
- [x] 步骤 2：点击“预约榜”子榜单
- [x] 步骤 3：返回排行/搜索页上文
