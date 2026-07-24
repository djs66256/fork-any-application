# 采集计划 — homepage-feed/menu-panel/recently-viewed/first-card

- **采集日期**：2026-07-24
- **采集目标**：验证菜单抽屉“最近在看”代表卡片点击后的真实承接页，并确认其与既有 `homepage-feed/full-watch/` 路径的关系。
- **入口路径**：`homepage-feed/menu-panel/recently-viewed/first-card/`
- **采集环境**：mobile / 红果 7.2.4.32 / Android 匿名态

## 操作步骤

- [x] 步骤 1：打开首页左上菜单抽屉，记录“最近在看”入口态。
  - 产物：`assets/2026-07-24-step-01-entry-context.png`
  - 产物：`assets/2026-07-24-step-01-entry-context.xml`
  - 观察：抽屉内可见三张续播卡；本轮按代表 case 只验证第一张卡。第一次误点到卡片上方空白区域，未发生跳转；第二次重采改为点击第一张卡中心偏下位置，命中真实卡片。

- [x] 步骤 2：点击第一张续播卡，记录目标页。
  - 产物：`assets/2026-07-24-step-02-target-page.png`
  - 产物：`assets/2026-07-24-step-02-target-page.xml`
  - 观察：第二次重采进入竖屏剧集播放页，顶部显示“第2集”、倍速与更多，右侧有收藏/评论/点赞/分享互动列，底部有“选集”“相关推荐”等操作区。

- [x] 步骤 3：执行返回，记录返回后的上下文。
  - 产物：`assets/2026-07-24-step-03-back-context.png`
  - 观察：返回后重新落回菜单抽屉上下文，抽屉保持打开，说明该续播入口从抽屉直接跳入播放页且支持回退到抽屉。

## 采集命令摘要

### 第一次采集（误触）
- 关键点击：`adb shell input tap 160 540`
- 结果：点击点位落在第一张卡片上方空白区域，未进入目标页；按返回后只是关闭抽屉并回到首页 Feed。

### 第二次采集（有效）
- 关键点击：`adb shell input tap 160 800`
- 结果：成功进入代表卡片对应的剧集播放页。

## 产物归档

### 最终采用的有效产物
- `assets/2026-07-24-step-01-entry-context.png` ← 由 `recently-viewed-first-card-step-01-entry-context-v2.png` 归档
- `assets/2026-07-24-step-01-entry-context.xml` ← 由 `recently-viewed-first-card-step-01-entry-context-v2.xml` 归档
- `assets/2026-07-24-step-02-target-page.png` ← 由 `recently-viewed-first-card-step-02-target-page-v2.png` 归档
- `assets/2026-07-24-step-02-target-page.xml` ← 原始文件内容为 `ERROR: could not get idle state.`，不作为结构化分析依据，仅保留归档说明
- `assets/2026-07-24-step-03-back-context.png` ← 由 `recently-viewed-first-card-step-03-back-context-v2.png` 归档

## 异常记录

- `step-02-target-page.xml` 在第二次采集时未成功导出 UI 树，文件内容仅为 `ERROR: could not get idle state.`；本轮目标页结构判断主要依据截图完成。
- 第一次采集的误触结果仅用于说明坐标修正过程，不纳入最终业务文档截图索引。
