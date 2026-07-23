# 采集：红果 — 剧场卡片详情承接页

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
| 采集范围 | 三级页面以内的剧场卡片点击路径：首页 → 剧场 → 首个剧集卡片 → 详情/播放承接页 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入剧场首屏

- **操作**：冷启动并切换到剧场页
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 314 2240 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-01-theater-ready.png`
- **观察要点**：记录首卡可点击状态和标题信息

*执行后由 subagent 填写：*
- **观察**：剧场页默认停留在“找剧”频道，顶部搜索框仍保留“我在废土世界种草莓”，右侧有“识剧”入口。当前首屏左上首卡为《暖心快递员》，同屏还可见《开学那天，我被当成传说》《父兄战死只剩我，可我是纨绔啊》《大庆憨王》等剧集卡片，说明剧场卡片列表可在匿名态直接浏览并点击。
- **截图文件**：`assets/2026-07-23-step-01-theater-ready.png`

### 步骤 2：点击左上首个剧集卡片

- **操作**：点击剧场页左上首个海报卡片
- **命令**：`adb shell input tap 276 734 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-02-card-target.png`
- **观察要点**：进入详情页/播放页/登录门槛中的哪一种，记录页面主结构

*执行后由 subagent 填写：*
- **观察**：点击《暖心快递员》卡片后，没有经过独立图文详情页，而是直接进入全屏播放承接页。顶部显示“第1集”、返回、倍速与更多操作，右侧保留点赞、评论、分享等互动列；底部出现剧名《暖心快递员》、首集简介摘要，以及“选集·已完结·全3集”抽屉栏。视频内容区域仍有明显花屏/彩条，但页面结构可识别。
- **截图文件**：`assets/2026-07-23-step-02-card-target.png`

### 步骤 3：在承接页点击首个核心操作

- **操作**：若承接页匿名可操作，点击首个核心 CTA（如播放、选集、简介展开）
- **命令**：`adb shell input tap 540 1860 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-card-secondary.png`
- **观察要点**：确认是否进入三级页、是否被登录拦截

*执行后由 subagent 填写：*
- **观察**：本次点击后视频切换为暂停态，画面中央出现播放三角图标，但没有跳转到新的三级页，也未触发登录门槛。由 XML 可见，页面仍停留在《暖心快递员》第1集播放页，底部摘要为“第1集｜快递员送餐途中偶遇老板晕倒…”，并继续保留“选集·已完结·全3集”抽屉栏，说明该坐标实际命中的是视频播放区域而非“选集”或其他按钮。
- **截图文件**：`assets/2026-07-23-step-03-card-secondary.png`

### 步骤 4：返回剧场首屏

- **操作**：连续返回至剧场首屏
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-theater.png`
- **观察要点**：返回路径、剧场列表是否恢复原位

*执行后由 subagent 填写：*
- **观察**：返回后回到剧场“找剧”主列表，但中间弹出一次系统级提示“再按一次退出红果”，同时叠加了“新用户权益待领取”弹窗，奖励内容包括“1.56元现金待领取”“16888金币”，主 CTA 为“去福利页领取”。该弹窗在两次返回后的首页状态中遮挡了部分列表，但底部 Tab 仍保持在“剧场”，说明返回成功且剧场页存在活动弹窗打断。
- **截图文件**：`assets/2026-07-23-step-04-back-theater.png`

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-theater-card-detail.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：`assets/2026-07-23-theater-card-detail.mp4`

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-theater-ready.png` | 截图 | 步骤 1 | 剧场首屏与目标卡片 |
| `assets/2026-07-23-step-02-card-target.png` | 截图 | 步骤 2 | 卡片承接页 |
| `assets/2026-07-23-step-03-card-secondary.png` | 截图 | 步骤 3 | 承接页核心操作后的状态 |
| `assets/2026-07-23-step-04-back-theater.png` | 截图 | 步骤 4 | 返回剧场首屏 |
| `assets/2026-07-23-theater-card-detail.mp4` | 录屏 | 步骤 2-4 | 剧场卡片承接链路录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 07:02:00 | 进入剧场首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 314 2240 && sleep 3` | 成功进入剧场主列表，左上首卡为《暖心快递员》 |
| 07:02:08 | 点击剧集卡片 | `adb shell input tap 276 734 && sleep 3` | 直接进入《暖心快递员》播放承接页 |
| 07:03:00 | 点击承接页核心操作 | `adb shell input tap 540 1860 && sleep 3` | 命中视频区域，页面切为暂停态，未跳转新页 |
| 07:03:08 | 返回剧场首屏 | `adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2` | 成功回剧场首页，但被“新用户权益待领取”弹窗覆盖部分页面 |

## 异常记录

- `step-02-card-target.xml` 未稳定导出成功，步骤 2 主要依据截图判读。
- 预设的“核心 CTA”坐标 `540 1860` 实际命中播放区域，触发的是暂停而不是“选集”或更深操作，因此本次未进入新的三级页。
- 返回剧场页时出现系统提示“再按一次退出红果”及活动弹窗“新用户权益待领取”，说明返回链路会受到福利弹窗干扰。
- 播放页视频区域继续存在花屏/彩条现象，但不影响识别剧名、集数和交互结构。
