# 采集规划：红果 — 排行页推荐榜子榜单

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/search/ranking/recommend-list |
| 采集范围 | 从搜索页排行模块进入“推荐榜”子榜单，记录推荐榜首屏与返回链路 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入排行页

- **操作**：从首页进入搜索页，再点击“排行”快捷入口
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 322 264 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-01-ranking-page.png`
- **观察要点**：确认已处于榜单页且“推荐榜”标签可见

*采集阶段回填：*
- **观察**：从首页进入搜索页后，再点击“排行”快捷入口，进入榜单页《红果热播榜》。在榜单页二级标签行中可见“推荐榜 / 热播榜 / 臻果榜 / 预约榜 / 分类”，其中初始默认选中的是“热播榜”，说明“推荐榜”需要进一步切换查看。
- **截图文件**：`assets/2026-07-24-step-01-ranking-page.png`

### 步骤 2：点击“推荐榜”子榜单

- **操作**：在排行页点击“推荐榜”标签，等待列表稳定
- **命令**：`adb shell input tap 133 555 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-02-recommend-list.png`
- **观察要点**：确认推荐榜条目结构、展示字段与当前选中状态

*采集阶段回填：*
- **观察**：点击“推荐榜”后，页面切换为《红果推荐榜》。顶部说明文案变为“基于红果观看/互动以及个人兴趣排序”，右侧指标从“热度”变为“推荐”，榜单条目中的辅助指标也从“收藏+点赞”切换为“热度+点赞”组合，说明推荐榜更强调个性化推荐分数而非全站综合热度。
- **截图文件**：`assets/2026-07-24-step-02-recommend-list.png`

### 步骤 3：返回排行页/搜索页上文

- **操作**：从推荐榜视图执行返回，确认回到排行页上文
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-03-back-ranking-context.png`
- **观察要点**：记录返回落点和排行页状态是否保留

*采集阶段回填：*
- **观察**：从推荐榜返回后直接回到搜索承接页，而不是回到热播榜页面。这说明“推荐榜”所在榜单页整体被视作搜索体系下的一层独立承接页，返回手势会直接退出整个排行页上下文。
- **截图文件**：`assets/2026-07-24-step-03-back-ranking-context.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-ranking-page.png` | 截图 | 步骤 1 | 排行页入口态 |
| `assets/2026-07-24-step-02-recommend-list.png` | 截图 | 步骤 2 | 推荐榜子榜单 |
| `assets/2026-07-24-step-03-back-ranking-context.png` | 截图 | 步骤 3 | 返回后的上文页面 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 13:53:00 | 进入排行页 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 322 264 && sleep 2` | 成功，进入《红果热播榜》 |
| 13:53:06 | 切换推荐榜 | `adb shell input tap 133 555 && sleep 2` | 成功，进入《红果推荐榜》 |
| 13:53:11 | 返回搜索页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 成功，直接返回搜索承接页 |

## 异常记录

无异常

## 执行状态

- [x] 步骤 1：进入排行页
- [x] 步骤 2：点击“推荐榜”子榜单
- [x] 步骤 3：返回排行页/搜索页上文
