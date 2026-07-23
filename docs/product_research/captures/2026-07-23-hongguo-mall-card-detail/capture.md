# 采集：红果 — 商城商品详情承接页

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-23 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 功能模块 | mall |
| 采集范围 | 三级页面以内的商城商品链路：首页 → 商城 → 首个商品卡片 → 商品详情或登录门槛 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入商城首屏

- **操作**：冷启动红果并切换到商城页
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 540 2240 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-01-mall-ready.png`
- **观察要点**：确认商城首屏稳定、商品卡片可点击

*执行后由 subagent 填写：*
- **观察**：商城首页匿名可达，顶部仍为搜索框、购物车与“我的订单 / 卡券红包 / 我的钱包 / 短剧同款 / 国家补贴”快捷入口，下方为直播与商品混排的双列瀑布流。首屏左上样本卡片为直播商品卡，底部店铺/主播名为“6555888”，热度数为“1343”；右侧样本卡片为商品静态卡，店铺名为“锦秀工厂店”，热度数为“646”。页面底部继续固定提示“登录可查看商品价格”，说明匿名态可浏览首屏商品卡，但价格与交易能力受限。
- **截图文件**：`assets/2026-07-23-step-01-mall-ready.png`

### 步骤 2：点击首个商品卡片

- **操作**：点击左侧首个商品卡片
- **命令**：`adb shell input tap 260 930 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-02-product-detail.png`
- **观察要点**：进入商品详情页、直播页或登录门槛中的哪一种

*执行后由 subagent 填写：*
- **观察**：点击左上首卡后，没有进入传统图文商品详情页，而是直接进入全屏直播带货承接页。页面顶部显示主播“6555888”、关注按钮、在线观众“1266”和关闭按钮；左下为实时评论流；右下悬浮商品卡展示“复古气质圆领印花衬衫…”，带“运费险 / 7天无理由退货 / ¥2? / 购买”等信息，右上角还有“热卖 x1321 / 主播讲解中”。说明商城首卡点击优先落到直播卖货场景，而不是商品图文详情页。
- **截图文件**：`assets/2026-07-23-step-02-product-detail.png`

### 步骤 3：在承接页点击首个核心 CTA

- **操作**：点击直播承接页右下商品卡中的首个核心 CTA“购买”
- **命令**：`adb shell input tap 880 2110 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-product-secondary.png`
- **观察要点**：是否进入三级页、是否要求登录或授权

*执行后由 subagent 填写：*
- **观察**：点击“购买”后，没有进入商品下单页、规格页或更深商品详情，而是直接跳到登录拦截页。该页为全屏登录页，标题是“登录 / 发现更多精彩短剧”，中部包含手机号输入框、“获取验证码”按钮、协议勾选区和底部抖音快捷登录按钮。说明在直播商品承接页内，交易 CTA 的首个门槛就是登录。
- **截图文件**：`assets/2026-07-23-step-03-product-secondary.png`

### 步骤 4：返回商城首屏

- **操作**：连续返回至商城首屏
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-mall.png`
- **观察要点**：返回路径和商城恢复状态

*执行后由 subagent 填写：*
- **观察**：第一次返回关闭全屏登录拦截页并退回直播承接页，第二次返回再退出直播页，成功回到商城首页。返回后顶部搜索框恢复为随机推荐词，双列商品流和底部“登录可查看商品价格 / 立即登录”浮层仍在，说明商城可恢复到匿名稳定态。
- **截图文件**：`assets/2026-07-23-step-04-back-mall.png`

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-mall-card-detail.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：`assets/2026-07-23-mall-card-detail.mp4`

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-mall-ready.png` | 截图 | 步骤 1 | 商城首屏 |
| `assets/2026-07-23-step-02-product-detail.png` | 截图 | 步骤 2 | 商品承接页 |
| `assets/2026-07-23-step-03-product-secondary.png` | 截图 | 步骤 3 | 商品详情二级动作结果 |
| `assets/2026-07-23-step-04-back-mall.png` | 截图 | 步骤 4 | 返回商城状态 |
| `assets/2026-07-23-mall-card-detail.mp4` | 录屏 | 步骤 2-4 | 商城商品链路录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 19:34:00 | 进入商城首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 540 2240 && sleep 3` | 成功进入商城首页，匿名可浏览商品流 |
| 19:34:10 | 点击商品卡片 | `adb shell input tap 260 930 && sleep 3` | 进入全屏直播卖货承接页，而非图文商品详情页 |
| 19:34:15 | 点击核心 CTA | `adb shell input tap 880 2110 && sleep 3` | 点击“购买”后直接进入全屏登录拦截页 |
| 19:34:20 | 返回商城首屏 | `adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2` | 先退回直播页，再退回商城首页稳定态 |

## 异常记录

- 商城首卡实际命中的是直播商品卡，进入后优先落到直播卖货页，而不是普通图文商品详情页。
- 直播页内可见商品悬浮卡及“购买”CTA，但点击后立即触发全屏登录拦截，未进入下单或规格三级页。
- 返回链路为“登录拦截页 → 直播页 → 商城首页”，可以恢复匿名稳定态。
