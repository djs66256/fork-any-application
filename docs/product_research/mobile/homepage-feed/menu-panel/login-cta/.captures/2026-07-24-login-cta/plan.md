# 采集规划：红果 — 菜单抽屉登录入口

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/menu-panel/login-cta |
| 采集范围 | 首页左上菜单抽屉顶部登录入口的入口态、登录承接页与返回后的上下文 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：记录菜单抽屉顶部登录入口

- **操作**：打开首页左上菜单抽屉，记录顶部登录 CTA 的原始位置与上下文
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 72 116 && sleep 2 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/login-cta-step-01-menu.png`
- **截图**：保存为 `assets/2026-07-24-step-01-menu-panel.png`
- **观察要点**：确认登录 CTA 在菜单抽屉中的位置、文案与和其它模块的层级关系

*采集阶段回填：*
- **观察**：菜单抽屉顶部展示匿名态头部，左侧为灰色头像占位，中间文案为“登录看完整信息”，右侧橙色主按钮为“立即登录”。登录入口位于“我的消息”之上，是抽屉中最高优先级操作。
- **截图文件**：`assets/2026-07-24-step-01-menu-panel.png`

### 步骤 2：进入登录承接页并记录表单结构

- **操作**：点击菜单抽屉顶部“立即登录”按钮，记录登录承接页
- **命令**：`adb shell input tap 550 152 && sleep 3 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/login-cta-step-02-login-cta.png && adb exec-out uiautomator dump /dev/tty | perl -pe 's/\x0D\x0A/\n/g' > docs/product_research/mobile/homepage-feed/menu-panel/login-cta-step-02-login-cta.xml`
- **截图**：保存为 `assets/2026-07-24-step-02-login-cta.png`
- **XML**：保存为 `assets/2026-07-24-step-02-login-cta.xml`
- **观察要点**：确认登录页的标题、输入项、协议勾选与第三方登录入口

*采集阶段回填：*
- **观察**：点击后进入全屏登录页，顶部左侧提供关闭按钮，主标题为“登录”，副标题为“发现更多精彩短剧”。页面采用手机号验证码登录：区号默认 `+86`，输入框提示“请输入您的手机号”，主按钮为“获取验证码”，未输入前按钮置灰。下方有协议勾选框及“用户协议 / 隐私政策 / 运营商服务协议”文案；底部提供抖音图标形式的第三方登录入口。XML 还显示手机号输入框初始获得焦点。
- **截图文件**：`assets/2026-07-24-step-02-login-cta.png`
- **XML 文件**：`assets/2026-07-24-step-02-login-cta.xml`

### 步骤 3：从登录页返回并确认回落上下文

- **操作**：在登录页点击返回键，观察页面回落位置
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/login-cta-step-03-back-context.png`
- **截图**：保存为 `assets/2026-07-24-step-03-back-context.png`
- **观察要点**：确认返回后是否回到菜单抽屉而不是首页首屏

*采集阶段回填：*
- **观察**：从登录页返回后，界面回到原菜单抽屉上下文，顶部匿名态登录 CTA 仍可见，右侧保留首页 Feed 背景。说明登录承接页以全屏覆盖方式打开，退出后回到抽屉而非关闭抽屉。
- **截图文件**：`assets/2026-07-24-step-03-back-context.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-menu-panel.png` | 截图 | 步骤 1 | 菜单抽屉顶部匿名态登录入口 |
| `assets/2026-07-24-step-02-login-cta.png` | 截图 | 步骤 2 | 登录承接页全屏表单 |
| `assets/2026-07-24-step-02-login-cta.xml` | XML | 步骤 2 | 登录承接页 UI 层级 |
| `assets/2026-07-24-step-03-back-context.png` | 截图 | 步骤 3 | 从登录页返回后的菜单抽屉上下文 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 16:24:00 | 打开菜单抽屉并记录登录 CTA | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 72 116 && sleep 2 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/login-cta-step-01-menu.png` | 成功，记录菜单抽屉顶部匿名态登录入口 |
| 16:24:10 | 点击“立即登录”并抓取登录页 | `adb shell input tap 550 152 && sleep 3 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/login-cta-step-02-login-cta.png && adb exec-out uiautomator dump /dev/tty | perl -pe 's/\x0D\x0A/\n/g' > docs/product_research/mobile/homepage-feed/menu-panel/login-cta-step-02-login-cta.xml` | 成功，记录登录页截图与 XML |
| 16:24:18 | 从登录页返回并记录上下文 | `adb shell input keyevent KEYCODE_BACK && sleep 2 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/login-cta-step-03-back-context.png` | 成功，确认返回后回到菜单抽屉 |
| 16:25:30 | 归档采集产物 | `cp login-cta-step-0{1,2,3}* homepage-feed/menu-panel/login-cta/.captures/2026-07-24-login-cta/assets/` | 成功，完成产物归档 |

## 异常记录

- 本轮未输入手机号，也未进入验证码发送阶段；当前仅覆盖登录承接页入口态与静态表单结构。
- 第三方登录当前只看到抖音图标入口，尚未点击验证授权链路。

## 执行状态

- [x] 步骤 1：记录菜单抽屉顶部登录入口
- [x] 步骤 2：进入登录承接页并记录表单结构
- [x] 步骤 3：从登录页返回并确认回落上下文
