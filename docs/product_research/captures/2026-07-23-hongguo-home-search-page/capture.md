# 采集：红果 — 首页搜索承接页

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-23 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 功能模块 | homepage-feed |
| 采集范围 | 三级页面以内的首页搜索入口：首页首屏 → 搜索页/搜索推荐页 → 首个匿名可达结果页或登录门槛 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入首页首屏

- **操作**：冷启动红果并等待首页稳定
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5`
- **截图**：保存为 `assets/2026-07-23-step-01-home-ready.png`
- **观察要点**：记录首页可操作状态和搜索入口位置

*执行后由 subagent 填写：*
- **观察**：首页首卡为《我觉醒了返利人生》，右上角搜索按钮可见且可点击；页面仍存在视频花屏现象，但标题、标签、互动数以及底部“观看完整漫剧·全75集”入口可识别。右侧同时出现“374金币”红包浮层，说明首页存在赚钱导流悬浮入口。
- **截图文件**：`assets/2026-07-23-step-01-home-ready.png`

### 步骤 2：点击搜索按钮进入搜索页

- **操作**：点击首页右上角搜索入口
- **命令**：`adb shell input tap 1008 118 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-search-page.png`
- **观察要点**：搜索页结构、是否带热搜/历史/推荐词

*执行后由 subagent 填写：*
- **观察**：进入独立搜索页，顶部为可编辑搜索框与“搜索”按钮，下方提供“识剧”“排行”等快捷入口；页面包含搜索历史（当前可见词为“青春甜宠”）、猜你想搜卡片，以及“搜榜/漫剧热搜榜/预约榜/热点话题榜”等推荐内容，匿名态可正常浏览。
- **截图文件**：`assets/2026-07-23-step-02-search-page.png`

### 步骤 3：执行一次关键词搜索

- **操作**：输入“修仙”并提交搜索
- **命令**：`adb shell input tap 260 118 && sleep 1 && adb shell input text xiuxian && adb shell input keyevent KEYCODE_ENTER && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-search-result.png`
- **观察要点**：结果页结构、结果类型、是否进入登录门槛

*执行后由 subagent 填写：*
- **观察**：本次实际提交的检索词为“我在废土世界种草莓”（搜索框已有残留文本，最终直接以当前词提交），结果页停留在“综合”标签，首屏返回短剧卡片、图文内容及相关推荐混合结果。首卡即命中同名短剧《我在废土世界种草莓》，旁边与下方展示科幻末世/都市脑洞类相关内容，未遇到登录门槛。
- **截图文件**：`assets/2026-07-23-step-03-search-result.png`

### 步骤 4：返回首页

- **操作**：连续返回直至首页首屏
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-home.png`
- **观察要点**：返回路径是否经过搜索页，首页恢复状态

*执行后由 subagent 填写：*
- **观察**：连续返回后回到首页流，当前展示的首卡仍为《我觉醒了返利人生》，搜索页已关闭；首页状态恢复正常，搜索入口与金币红包浮层仍在，说明从搜索结果返回会直接回到首页推荐流而非停留在搜索页。
- **截图文件**：`assets/2026-07-23-step-04-back-home.png`

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-home-search-page.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-home-ready.png` | 截图 | 步骤 1 | 首页可搜索状态 |
| `assets/2026-07-23-step-02-search-page.png` | 截图 | 步骤 2 | 搜索页首屏 |
| `assets/2026-07-23-step-03-search-result.png` | 截图 | 步骤 3 | 搜索结果页 |
| `assets/2026-07-23-step-04-back-home.png` | 截图 | 步骤 4 | 返回首页状态 |
| `assets/2026-07-23-home-search-page.mp4` | 录屏 | 步骤 2-4 | 首页搜索链路录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入首页首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5` | 待执行 |
| HH:MM:SS | 进入搜索页 | `adb shell input tap 1008 118 && sleep 2` | 待执行 |
| HH:MM:SS | 搜索关键词 | `adb shell input tap 260 118 && sleep 1 && adb shell input text xiuxian && adb shell input keyevent KEYCODE_ENTER && sleep 3` | 待执行 |
| HH:MM:SS | 返回首页 | `adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
