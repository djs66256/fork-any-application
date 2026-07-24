# 采集规划：红果 — 游戏中心更多入口

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/menu-panel/game-center/more-entry |
| 采集范围 | 菜单抽屉游戏中心右上角“更多”入口的点击结果、是否受登录拦截以及返回上下文 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：记录游戏中心“更多”入口所在位置

- **操作**：打开首页左上菜单抽屉，定位游戏中心模块的右上角“更多”入口
- **命令**：复用 `homepage-feed/menu-panel/game-center/.captures/2026-07-24-game-center/assets/2026-07-24-step-01-game-center.png`
- **截图**：保存为 `assets/2026-07-24-step-01-entry-context.png`
- **观察要点**：确认“更多”位于游戏中心标题右侧，属于模块级入口而非单个游戏卡片

*采集阶段回填：*
- **观察**：游戏中心标题右侧有“更多”文字和右箭头，入口与四个游戏快捷入口分离，说明它承接的是模块级扩展内容，而不是某一具体游戏详情。
- **截图文件**：`assets/2026-07-24-step-01-entry-context.png`

### 步骤 2：点击“更多”并记录拦截结果

- **操作**：点击游戏中心右上角“更多”入口
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 72 116 && sleep 2 && adb shell input tap 736 1172 && sleep 4 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/game-center-more-entry-step-03-target.png && adb exec-out uiautomator dump /dev/tty | perl -pe 's/\x0D\x0A/\n/g' > docs/product_research/mobile/homepage-feed/menu-panel/game-center-more-entry-step-03-target.xml`
- **截图**：保存为 `assets/2026-07-24-step-02-login-intercept.png`
- **XML**：保存为 `assets/2026-07-24-step-02-login-intercept.xml`
- **观察要点**：确认该入口是进入独立页还是被登录门槛拦截

*采集阶段回填：*
- **观察**：点击“更多”后并未进入游戏列表页，而是直接弹出与顶部 `login-cta` 相同的全屏手机号登录页。页面仍为“登录 / 发现更多精彩短剧”，包含 `+86`、手机号输入框、获取验证码按钮、协议勾选区与抖音登录入口。说明匿名用户访问游戏中心“更多”时会被身份门槛拦截。
- **截图文件**：`assets/2026-07-24-step-02-login-intercept.png`
- **XML 文件**：`assets/2026-07-24-step-02-login-intercept.xml`

### 步骤 3：从登录拦截页返回并确认回落位置

- **操作**：从登录拦截页返回
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/game-center-more-entry-step-04-back-context.png && adb exec-out uiautomator dump /dev/tty | perl -pe 's/\x0D\x0A/\n/g' > docs/product_research/mobile/homepage-feed/menu-panel/game-center-more-entry-step-04-back-context.xml`
- **截图**：保存为 `assets/2026-07-24-step-03-back-context.png`
- **XML**：保存为 `assets/2026-07-24-step-03-back-context.xml`
- **观察要点**：确认返回后是否回到菜单抽屉中的游戏中心上下文

*采集阶段回填：*
- **观察**：返回后重新回到菜单抽屉，游戏中心模块及其“更多”入口仍可见，右侧保留首页 Feed 背景。说明“更多”触发的是抽屉上下文中的登录拦截，而不是独立跳转后关闭抽屉。
- **截图文件**：`assets/2026-07-24-step-03-back-context.png`
- **XML 文件**：`assets/2026-07-24-step-03-back-context.xml`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-entry-context.png` | 截图 | 步骤 1 | 游戏中心“更多”入口所在模块上下文 |
| `assets/2026-07-24-step-02-login-intercept.png` | 截图 | 步骤 2 | 点击“更多”后的登录拦截页 |
| `assets/2026-07-24-step-02-login-intercept.xml` | XML | 步骤 2 | 登录拦截页 UI 层级 |
| `assets/2026-07-24-step-03-back-context.png` | 截图 | 步骤 3 | 从登录拦截页返回后的菜单抽屉上下文 |
| `assets/2026-07-24-step-03-back-context.xml` | XML | 步骤 3 | 返回后菜单抽屉的 UI 层级 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 16:18:00 | 复用游戏中心模块截图 | `cp homepage-feed/menu-panel/game-center/.captures/2026-07-24-game-center/assets/2026-07-24-step-01-game-center.png homepage-feed/menu-panel/game-center/more-entry/.captures/2026-07-24-more-entry/assets/2026-07-24-step-01-entry-context.png` | 成功，记录“更多”入口所在位置 |
| 16:14:00 | 点击游戏中心“更多” | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 72 116 && sleep 2 && adb shell input tap 736 1172 && sleep 4 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/game-center-more-entry-step-03-target.png && adb exec-out uiautomator dump /dev/tty | perl -pe 's/\x0D\x0A/\n/g' > docs/product_research/mobile/homepage-feed/menu-panel/game-center-more-entry-step-03-target.xml` | 成功，结果为登录拦截页 |
| 16:16:00 | 从登录拦截页返回 | `adb shell input keyevent KEYCODE_BACK && sleep 2 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/game-center-more-entry-step-04-back-context.png && adb exec-out uiautomator dump /dev/tty | perl -pe 's/\x0D\x0A/\n/g' > docs/product_research/mobile/homepage-feed/menu-panel/game-center-more-entry-step-04-back-context.xml` | 成功，返回菜单抽屉上下文 |
| 16:18:30 | 归档 more-entry 产物 | `cp game-center-more-entry-step-* homepage-feed/menu-panel/game-center/more-entry/.captures/2026-07-24-more-entry/assets/` | 成功，完成产物归档 |

## 异常记录

- 匿名态下，“更多”并未进入可见的游戏列表承接页，而是被统一登录页拦截。
- 由于存在登录门槛，本轮未能看到登录后的真实游戏中心完整列表；后续若具备登录态，可继续补采登录后页面。

## 执行状态

- [x] 步骤 1：记录游戏中心“更多”入口所在位置
- [x] 步骤 2：点击“更多”并记录拦截结果
- [x] 步骤 3：从登录拦截页返回并确认回落位置
