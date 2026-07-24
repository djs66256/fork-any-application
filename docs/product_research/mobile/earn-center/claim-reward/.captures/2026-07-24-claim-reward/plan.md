# 采集计划 — earn-center/claim-reward

- **采集日期**：2026-07-24
- **采集目标**：采集红果底部“赚钱”Tab 首屏代表奖励领取入口的点击承接与返回上下文，确认其是否进入奖励详情、登录页或内容播放页。
- **入口路径**：`earn-center/claim-reward/`
- **采集环境**：mobile / 红果 7.2.4.32 / Android 匿名态

## 操作步骤

- [x] 步骤 1：冷启动进入首页，切换到底部“赚钱”Tab，记录首屏页面。
  - 观察：成功进入“赚钱”Tab，首屏为任务/收益中心，展示现金收益、新手任务、连续看短剧福利和立即领取按钮。
- [x] 步骤 2：点击首屏代表奖励领取入口，记录目标页。
  - 观察：点击右侧“立即领取”类奖励入口后，系统直接进入竖屏短剧播放承接页；页面含标题、标签、互动列和底部“观看完整漫剧·全84集”入口。
- [x] 步骤 3：从目标页执行返回，记录返回后的赚钱上下文。
  - 观察：返回后重新落回“赚钱”Tab 首屏，底部“赚钱”仍保持高亮选中。

## 产物清单

- `assets/2026-07-24-step-01-entry-context.png`
- `assets/2026-07-24-step-01-entry-context.xml`
- `assets/2026-07-24-step-02-target-page.png`
- `assets/2026-07-24-step-02-target-page.xml`
- `assets/2026-07-24-step-03-back-context.png`
- `assets/2026-07-24-step-03-back-context.xml`

## 异常记录

- 三次 uiautomator dump 均返回 `ERROR: could not get idle state.`，因此本轮依赖截图做页面判定。
- 目标页视频区域出现花屏，但其余关键 UI 结构足够识别为播放承接页。
