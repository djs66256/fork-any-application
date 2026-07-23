# 采集：红果 — 剧场搜索与识图入口

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-23 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 功能模块 | theater |
| 采集范围 | 三级页面以内的剧场搜索：首页 → 剧场 → 搜索/截图识别短剧入口 → 搜索页或登录门槛 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入剧场首屏

- **操作**：冷启动并切换到剧场页
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 314 2240 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-01-theater-ready.png`
- **观察要点**：搜索框和识图入口可见状态

*执行后由 subagent 填写：*
- **观察**：剧场页默认停留在“找剧”频道，顶部搜索框中残留检索词“我在废土世界种草莓”，并带有“漫剧”标签；右侧可见“识剧”入口。首屏功能入口含“筛选”“排行榜”“新剧”“预约”，当前可见卡片样本包括《开学那天，我被当成传说》《万民清碑》《我带着现代武器制霸古代》《穿越成废材皇子，我坐拥雄兵百万》。
- **截图文件**：`assets/2026-07-23-step-01-theater-ready.png`

### 步骤 2：点击剧场搜索框

- **操作**：点击剧场搜索框进入搜索页
- **命令**：`adb shell input tap 260 116 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-search-page.png`
- **观察要点**：搜索页结构、推荐词、历史词或热搜区域

*执行后由 subagent 填写：*
- **观察**：点击后进入独立搜索页，顶部为可编辑搜索框与“搜索”按钮；搜索框仍保留残留词“我在废土世界种草莓”。搜索页快捷入口包括“识剧”“排行”“上新”“演员”“分类”，中部展示搜索历史“都市日常 / 我在废土世界种草莓 / 青春甜宠”。下方还有“猜你想搜”、热搜榜及大量推荐内容，说明剧场搜索页匿名态可正常浏览。
- **截图文件**：`assets/2026-07-23-step-02-search-page.png`

### 步骤 3：返回后点击“截图识别短剧”入口

- **操作**：返回剧场首屏，再点击右侧识图入口
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input tap 872 116 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-image-recognition.png`
- **观察要点**：是否请求系统权限、上传截图、登录门槛或功能介绍页

*执行后由 subagent 填写：*
- **观察**：点击“识剧”后先触发 Android 系统相册/媒体权限弹窗，请求“Allow 红果免费短剧 to access photos and videos on this device?”；本次选择拒绝后，应用内继续弹出“权限申请”提示，文案为“在设置-应用-红果免费短剧-权限中开启存储权限，以正常使用相关功能”，提供“取消 / 去设置”。因此识剧链路的首个门槛不是登录，而是系统存储权限；在未授权前无法进入真正的截图上传/识别页面。
- **截图文件**：`assets/2026-07-23-step-03-image-recognition.png`

### 步骤 4：返回剧场首屏

- **操作**：返回到剧场页
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-theater.png`
- **观察要点**：返回路径、搜索与识图状态恢复情况

*执行后由 subagent 填写：*
- **观察**：关闭应用内“权限申请”弹窗后，成功回到剧场“找剧”主列表。页面顶部搜索框中的词被改写为“匠心映晚晴”，右侧仍保留“识剧”入口；功能入口恢复为“筛选 / 排行榜 / 新剧 / 预约”，可见剧集列表样本包括《开学那天，我被当成传说》《万民清碑》《我带着现代武器制霸古代》《穿越成废材皇子，我坐拥雄兵百万》。说明识剧权限链路退出后可正常回剧场页，但会污染顶部搜索词状态。
- **截图文件**：`assets/2026-07-23-step-04-back-theater.png`

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-theater-search.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：`assets/2026-07-23-theater-search.mp4`

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-theater-ready.png` | 截图 | 步骤 1 | 剧场首屏搜索入口状态 |
| `assets/2026-07-23-step-02-search-page.png` | 截图 | 步骤 2 | 剧场搜索页 |
| `assets/2026-07-23-step-03-image-recognition.png` | 截图 | 步骤 3 | 识图找剧承接层 |
| `assets/2026-07-23-step-04-back-theater.png` | 截图 | 步骤 4 | 返回剧场首屏 |
| `assets/2026-07-23-theater-search.mp4` | 录屏 | 步骤 2-4 | 剧场搜索链路录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 07:06:00 | 进入剧场首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 314 2240 && sleep 3` | 成功进入剧场“找剧”首屏，搜索框与识剧入口可见 |
| 07:06:08 | 打开剧场搜索页 | `adb shell input tap 260 116 && sleep 2` | 成功进入搜索页，可见历史词与快捷入口 |
| 07:06:18 | 打开识图入口 | `adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input tap 872 116 && sleep 3` | 先触发系统媒体权限弹窗，拒绝后进入应用内“权限申请”提示 |
| 07:09:00 | 返回剧场首屏 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 关闭权限提示后成功返回剧场主列表 |

## 异常记录

- 识剧入口触发的第一道门槛是系统存储/相册权限，而不是登录门槛；拒绝系统权限后应用内会追加弹出“权限申请”提示。
- 由于系统权限弹窗与应用内权限提示叠加，本次未进入真实的截图上传/识别页，仅记录到权限门槛。
- 关闭权限提示返回剧场页后，顶部搜索词被污染为“匠心映晚晴”，不再保持原词“我在废土世界种草莓”。
