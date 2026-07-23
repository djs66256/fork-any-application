# AI 操作与自动化 — Android

> 本文档定义 Android 端 AI agent 可执行的设备操作与自动化能力。

---

## 1. ADB 操作

以下所有命令默认已通过 `adb devices` 确认设备已连接。

### 1.1 设备管理

```bash
# 列出已连接设备（含模拟器）
adb devices
# 输出示例：
# List of devices attached
# emulator-5554   device
# 192.168.1.10:5555   device

# 详细设备信息
adb devices -l
# 输出附加 transport_id 和 product/model 信息

# USB 连接
adb usb

# 通过 IP 连接（需先 USB 连接后启用 TCP/IP）
adb tcpip 5555
adb connect 192.168.1.10:5555

# 断开 TCP/IP 连接
adb disconnect 192.168.1.10:5555

# 重启 adb server（调试 adb 自身问题时用）
adb kill-server && adb start-server

# 获取设备属性
adb shell getprop ro.product.model        # 设备型号
adb shell getprop ro.build.version.sdk    # SDK 版本
adb shell getprop ro.build.version.release # Android 版本号

# 获取序列号
adb get-serialno
```

### 1.2 应用管理

```bash
# 安装 APK（覆盖安装，-r 允许降级，-t 允许测试包，-d 允许降级）
adb install -r -t -d app/build/outputs/apk/debug/app-debug.apk

# 从 SD 卡安装（用于大 APK）
adb push app-debug.apk /sdcard/
adb shell pm install -r /sdcard/app-debug.apk

# 卸载应用
adb uninstall com.djs66256.short_drama

# 启动应用：指定包名 + Activity 类名
adb shell am start -n com.djs66256.short_drama/.MainActivity

# 通过 Deep Link 启动
adb shell am start -a android.intent.action.VIEW -d "djsdrama://video/abc123"

# 通过 Action 启动
adb shell am start -a android.intent.action.VIEW -d "djsdrama://home"

# 通过 Intent 传参启动
adb shell am start -n com.djs66256.short_drama/.MainActivity \
    --es "debug_screen" "VideoDetail" \
    --es "debug_video_id" "test_video_001"

# 强制停止应用
adb shell am force-stop com.djs66256.short_drama

# 清除应用数据（等效于"设置 → 应用 → 清除数据"）
adb shell pm clear com.djs66256.short_drama

# 清除应用数据但保留外部存储
adb shell pm clear --cache-only com.djs66256.short_drama

# 查看当前前台 Activity
adb shell dumpsys activity activities | grep "mResumedActivity"

# 查看应用进程 PID
adb shell pidof com.djs66256.short_drama

# 查看运行中的应用信息
adb shell dumpsys package com.djs66256.short_drama | grep -E "versionName|versionCode|userId"

# 授予权限（调试时需要）
adb shell pm grant com.djs66256.short_drama android.permission.WRITE_EXTERNAL_STORAGE
adb shell pm grant com.djs66256.short_drama android.permission.POST_NOTIFICATIONS

# 撤销权限
adb shell pm revoke com.djs66256.short_drama android.permission.CAMERA

# 列出应用所有权限
adb shell dumpsys package com.djs66256.short_drama | grep -A20 "requested permissions"
```

### 1.3 截图与录屏

**截图**：

```bash
# 标准截图（PNG 格式，保存在设备 /sdcard/ 中）
adb shell screencap -p /sdcard/screenshot.png

# 拉取到本地
adb pull /sdcard/screenshot.png ./screenshots/screen_$(date +%Y%m%d_%H%M%S).png

# 一键截图（合并命令，macOS/Linux）
adb exec-out screencap -p > screenshot_$(date +%Y%m%d_%H%M%S).png

# 指定显示 ID（多屏场景，默认 display 0）
adb shell screencap -d 0 -p /sdcard/screenshot.png
```

**录屏**：

```bash
# 开始录屏（最长 180 秒，--bit-rate 单位 bps）
adb shell screenrecord /sdcard/demo.mp4 \
    --size 1080x1920 \
    --bit-rate 8000000 \
    --time-limit 30

# 停止录屏：Ctrl+C（当前 shell）或
adb shell killall screenrecord

# 拉取到本地
adb pull /sdcard/demo.mp4 ./recordings/demo_$(date +%Y%m%d_%H%M%S).mp4

# 显示 Touch 点击位置（方便录制时标注操作）
adb shell settings put system show_touches 1
adb shell settings put system pointer_location 1
# 关闭
adb shell settings put system show_touches 0
adb shell settings put system pointer_location 0
```

### 1.4 触摸与输入

**基础触摸操作**：

```bash
# 点击屏幕坐标 (x, y)
adb shell input tap 540 960

# 滑动（从 x1,y1 到 x2,y2，duration 单位毫秒）
adb shell input swipe 540 1600 540 400 300

# 长按
adb shell input swipe 540 960 540 960 1500

# 双击（两次快速点击）
adb shell input tap 540 960 && sleep 0.1 && adb shell input tap 540 960

# 输入文本（不支持中文时用 adb shell ime 切换输入法）
adb shell input text "test_search_keyword"

# 发送按键事件
adb shell input keyevent KEYCODE_BACK       # 返回
adb shell input keyevent KEYCODE_HOME       # Home
adb shell input keyevent KEYCODE_APP_SWITCH # 多任务
adb shell input keyevent KEYCODE_ENTER      # 回车
adb shell input keyevent KEYCODE_DEL        # 删除
adb shell input keyevent KEYCODE_DPAD_UP    # 方向键上
adb shell input keyevent KEYCODE_DPAD_DOWN  # 方向键下
adb shell input keyevent KEYCODE_DPAD_LEFT  # 方向键左
adb shell input keyevent KEYCODE_DPAD_RIGHT # 方向键右
adb shell input keyevent KEYCODE_TAB        # Tab（焦点切换）
```

**常用按键 KeyCode 速查**：

| 按键 | Keycode | 用途 |
|------|---------|------|
| 返回 | `KEYCODE_BACK` (4) | 返回上一页 |
| Home | `KEYCODE_HOME` (3) | 回到桌面 |
| 多任务 | `KEYCODE_APP_SWITCH` (187) | 打开最近任务 |
| 电源 | `KEYCODE_POWER` (26) | 锁屏/唤醒 |
| 音量+ | `KEYCODE_VOLUME_UP` (24) | 音量加 |
| 音量- | `KEYCODE_VOLUME_DOWN` (25) | 音量减 |
| 回车 | `KEYCODE_ENTER` (66) | 确认输入 |
| 空格 | `KEYCODE_SPACE` (62) | 空格 |

**模拟文本输入（含中文）**：

```bash
# 切换到 Android 测试输入法并输入中文
adb shell ime set com.android.adbkeyboard/.AdbIME
adb shell am broadcast -a ADB_INPUT_TEXT --es msg "测试关键词"
# 恢复默认输入法
adb shell ime set com.google.android.inputmethod.pinyin/.PinyinIME
```

**注意**：使用 `adb shell input text` 输入中文需要设备安装 ADBKeyboard 输入法（开源项目），或通过 `uiautomator` 设置文本。

### 1.5 系统操作

```bash
# 旋转屏幕（0=竖屏, 1=横左, 2=倒置, 3=横右）
adb shell settings put system user_rotation 0  # 锁定竖屏
adb shell settings put system user_rotation 1  # 锁定横屏
# 取消锁定（自动旋转）
adb shell settings put system accelerometer_rotation 1

# 屏幕亮度（0-255）
adb shell settings put system screen_brightness 128

# 获取屏幕尺寸
adb shell wm size
# 输出示例：Physical size: 1080x2400

# 获取屏幕密度
adb shell wm density

# 语言切换
adb shell settings put system system_locales zh-CN   # 中文
adb shell settings put system system_locales en-US    # 英文

# 日期时间（设置到指定时间，格式 HHMMSS)
# 需要 root 或系统权限

# 设置/取消飞行模式（需要 root 或系统权限）
adb shell settings put global airplane_mode_on 1
adb shell am broadcast -a android.intent.action.AIRPLANE_MODE

# 开启/关闭 Wi-Fi
adb shell svc wifi enable
adb shell svc wifi disable

# 获取当前网络类型
adb shell dumpsys connectivity | grep "NetworkAgentInfo"
```

---

## 2. UI 自动化

### 2.1 UIAutomator

UIAutomator 可用于在不写代码的情况下探查当前界面的 UI 层次结构，辅助 AI agent 理解界面布局。

**Dump UI 树**：

```bash
# 导出当前界面 UI 树（XML 格式）
adb shell uiautomator dump /sdcard/ui_hierarchy.xml

# 拉取到本地
adb pull /sdcard/ui_hierarchy.xml ./ui_snapshots/ui_$(date +%Y%m%d_%H%M%S).xml

# 查看 UI 树（直接在 shell 中输出，部分设备支持）
adb shell uiautomator dump /dev/tty && adb shell cat /sdcard/ui_hierarchy.xml
```

**解析 UI 树信息**：AI agent 可通过解析 XML 获取以下关键信息：

- 搜索关键词出现的位置：`grep -i "搜索" ui_hierarchy.xml`
- 所有可点击元素：`grep 'clickable="true"' ui_hierarchy.xml`
- 所有文本元素 text 属性：`grep -oP 'text="[^"]*"' ui_hierarchy.xml`
- 按钮文本（如确认、取消、登录等）

**通过 UI 元素属性点击**（需编写 UiAutomator 测试代码）：

```kotlin
// Espresso/UiAutomator 示例
val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
// 通过文本查找并点击
device.findObject(UiSelector().text("登录")).click()
// 通过 resource-id 查找
device.findObject(UiSelector().resourceId("com.djs66256.short_drama:id/btn_login")).click()
// 通过 contentDescription 查找
device.findObject(UiSelector().description("搜索")).click()
```

**纯命令行 UI 操作方案**：结合 `uiautomator dump` + `input tap <x> <y>` 实现无代码操作：
```bash
# 1. 切换到目标页面
adb shell am start -n com.djs66256.short_drama/.MainActivity
sleep 2
# 2. Dump UI 树
adb shell uiautomator dump /sdcard/ui.xml
adb pull /sdcard/ui.xml
# 3. 解析 bounds 获取坐标（如 bounds="[0,100][1080,200]" 表示左上角(0,100)，右下角(1080,200)）
# 计算中心点 X=(0+1080)/2=540, Y=(100+200)/2=150
# 4. 点击
adb shell input tap 540 150
```

### 2.2 Espresso

Espresso 用于编写可重复运行的 UI 自动化测试。

**核心 API 示例**（Kotlin）：

```kotlin
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clickFirstVideo_navigatesToDetail() {
        composeTestRule.setContent {
            ShortDramaTheme {
                HomeScreen(viewModel = testViewModel)
            }
        }

        // 断言：首页 Feed 可见
        composeTestRule
            .onNodeWithTag("home_feed_list")
            .assertIsDisplayed()

        // 点击第一个视频卡片
        composeTestRule
            .onAllNodesWithTag("video_card")
            .onFirst()
            .performClick()

        // 断言：已跳转到视频详情页
        composeTestRule
            .onNodeWithTag("video_detail_screen")
            .assertIsDisplayed()
    }
}
```

**Espresso 与 Compose 集成注意**：
- 使用 `createComposeRule()` 而非 `ActivityScenarioRule`（后者用于 View 体系）。
- 为关键 Composable 添加 `Modifier.testTag("xxx")` 以在测试中定位。
- `testTag` 仅在测试 Build 中生效，不影响生产代码（使用 `BuildConfig.DEBUG` 条件编译以避免始终携带）。
- 不推荐使用 `onNodeWithText()` 来定位（国际化后文字会变），优先使用 `testTag`。

---

## 3. 截图与视觉比对

### 3.1 截图采集

AI agent 对指定页面采样的标准流程：

```bash
# Step 1：通过 Deep Link 或 am start 进入目标页面
adb shell am start -a android.intent.action.VIEW -d "djsdrama://home"
sleep 3  # 等待页面渲染完成

# Step 2：截图
adb exec-out screencap -p > captures/01_home_main.png

# Step 3：执行交互操作（如点击搜索图标）
adb shell input tap 1000 200  # 搜索图标坐标（需根据 Layout Inspector 确认）
sleep 2

# Step 4：再次截图
adb exec-out screencap -p > captures/02_home_search.png

# Step 5：返回
adb shell input keyevent KEYCODE_BACK
sleep 1
adb exec-out screencap -p > captures/03_home_back.png
```

**截图命名约定**：
```
captures/<feature>/
├── 00_initial_state.png        # 页面初始状态
├── 01_after_action_a.png       # 执行操作 A 后
├── 02_after_action_b.png       # 执行操作 B 后
└── XX_final_state.png          # 最终状态
```

**等待策略**：
- 页面跳转后等待 2~3 秒（`sleep 3`），确保动画和网络加载完成。
- 对于网络请求依赖的页面，使用轮询检查替代固定等待：
  ```bash
  # 等待特定 UI 元素出现（如加载完成后的列表）
  for i in $(seq 1 10); do
      adb shell uiautomator dump /sdcard/check.xml && \
      adb pull /sdcard/check.xml -q && \
      grep -q "video_card" check.xml && break
      sleep 1
  done
  ```
- 下拉刷新后等待 2 秒再截图。

### 3.2 设计稿比对

**比对流程**：

1. **导出设计稿**：从 Figma 导出目标页面的 PNG（与设备分辨率相同，1080x 宽度标准）。
2. **截取应用截图**：使用上述截图流程获取同页面截图。
3. **叠图比对**：使用 ImageMagick 或 Python PIL 进行像素级比对。

**ImageMagick 命令行比对示例**：

```bash
# 叠图对比（半透明叠加，肉眼发现差异）
composite -dissolve 50% design.png screenshot.png comparison.png

# 计算差异（生成差异热力图）
compare -metric AE design.png screenshot.png diff.png
# -metric AE 返回绝对误差像素数（Absolute Error count）

# 生成差异图（红色高亮差异区域）
compare -compose src design.png screenshot.png highlighted_diff.png
```

**Python 像素比对脚本示例**：

```python
from PIL import Image
import numpy as np

design = Image.open("design.png").resize((1080, 2400))
screenshot = Image.open("screenshot.png").resize((1080, 2400))

diff = np.abs(np.array(design).astype(float) - np.array(screenshot).astype(float))
diff_pct = np.mean(diff > 30)  # 像素差异超过 30 视为不一致

print(f"差异像素占比: {diff_pct:.2%}")
# 阈值：< 5% 认为基本一致，5-15% 为部分偏差，> 15% 为显著不一致
```

**自动化比对待检项目**：
- 间距检查：卡片间距、边距是否与设计一致
- 字号/字重：标题、正文 (使用 OCR 提取文字属性)
- 颜色校验：关键色号是否匹配（如品牌色 `#6C5CE7`）
- 图标位置：返回按钮、Tab 图标相对位置

---

## 4. 日志采集

### 4.1 实时日志

```bash
# 实时查看所有日志（时间戳格式）
adb logcat -v time

# 带线程信息的实时日志
adb logcat -v threadtime

# 实时查看并同时写入文件（tee 方式，macOS/Linux）
adb logcat -v time 2>&1 | tee app_log_$(date +%Y%m%d_%H%M%S).log

# 清除旧日志后再开始收集（减少无关历史）
adb logcat -c && adb logcat -v time > session_log.txt
```

### 4.2 日志过滤

**按应用 PID 过滤（只收集应用日志）**：

```bash
# 获取 PID
PID=$(adb shell pidof com.djs66256.short_drama)
# 只显示应用日志
adb logcat -v time --pid=$PID
```

**按优先级过滤**：

```bash
# 只收集 WARN 及以上级别（跳过 Debug/Info 噪音）
adb logcat -v time *:W

# 收集特定 TAG：SD_ 系列（ShortDrama 应用日志）+ 系统 ERROR
adb logcat -v time SD_*:V *:E

# 排除无关注释（如蓝牙、WiFi 扫描）
adb logcat -v time Bluetooth*:S WiFi*:S *:V
```

**按关键词过滤**：

```bash
# 包含 "crash" 或 "Error" 的行
adb logcat -v time | grep -E "crash|Error|FATAL|Exception"

# 按正则匹配所有 ViewModel 日志
adb logcat -v time | grep -E "SD_.*VM"

# 按正则匹配网络请求日志
adb logcat -v time | grep -E "SD_Net|OkHttp|Retrofit"

# 排除 logcat 自身干扰
adb logcat -v time | grep -v "logcat"
```

**组合过滤模板**：

```bash
# 完整排查模板：应用日志（WARN+）+ 网络日志 + 崩溃相关
adb logcat -v threadtime \
    -s "SD_*:*" \
    "AndroidRuntime:E" \
    "ActivityManager:I" \
    "System.err:W" \
    "*:S"
```

### 4.3 Bug Report

Bug Report 是 Android 系统级的完整诊断快照，包含 logcat、dumpsys、进程信息等所有诊断数据。

**导出 Bug Report**：

```bash
# 标准导出（ZIP 格式，包含所有诊断信息）
adb bugreport bugreport_$(date +%Y%m%d_%H%M%S).zip

# 仅导出 logcat + dumpsys（不含截屏和 ANR traces，体积更小）
adb bugreport --noprogress bugreport_no_screenshots.zip
```

**Bug Report 内容解析**：

Bug Report 解压后的关键文件和用途：

| 文件 | 内容 | 排查场景 |
|------|------|---------|
| `bugreport-xxx.txt` | logcat、dumpsys 全文 | 全文搜索崩溃、ANR、异常信息 |
| `FS/data/anr/` | ANR trace 文件 | ANR 问题定位（主线程堆栈） |
| `FS/data/tombstones/` | Native 崩溃墓碑 | C/C++ 层崩溃（如媒体解码） |
| `dumpstate_board.txt` | 内核日志 | 底层硬件问题 |
| `version.txt` | 版本信息 | 确认 build ID、Radio 版本 |

**快速解析技巧**：

```bash
# 解压
unzip bugreport.zip -d bugreport/

# 搜索崩溃
grep -n "FATAL EXCEPTION" bugreport/bugreport-*.txt

# 搜索 ANR
grep -n "ANR in" bugreport/bugreport-*.txt

# 搜索 ShortDrama 应用相关日志
grep -n "com.djs66256.short_drama" bugreport/bugreport-*.txt

# 搜索内存不足 (OOM)
grep -n "Out of memory" bugreport/bugreport-*.txt
```

---

## 5. 模拟器管理

### 5.1 创建与配置

```bash
# 列出已安装的系统镜像
sdkmanager --list | grep "system-images"

# 列出已创建的 AVD
avdmanager list avd

# 推荐：下载 API 34 镜像（适配竖屏短剧）
sdkmanager "system-images;android-34;google_apis;arm64-v8a"
sdkmanager "platforms;android-34"

# 创建自定义 AVD（竖屏手机，1080x2400，4GB RAM）
avdmanager create avd \
    -n shortdrama_test \
    -k "system-images;android-34;google_apis;arm64-v8a" \
    -d "pixel_6" \
    --force

# 通过 config.ini 自定义硬件配置（创建后修改）
# 文件位置：~/.android/avd/shortdrama_test.avd/config.ini
cat >> ~/.android/avd/shortdrama_test.avd/config.ini <<EOF
hw.ramSize=4096            # 4GB 内存
hw.keyboard=yes            # 启用物理键盘
disk.dataPartition.size=8G # 8GB 存储
hw.camera.back=webcam0     # 使用摄像头
hw.camera.front=emulated
hw.lcd.width=1080
hw.lcd.height=2400
hw.lcd.density=420
showDeviceFrame=no         # 不显示模拟器外壳
EOF
```

**多 AVD 并行**：为不同测试场景创建专用 AVD：

```bash
# 中文环境 AVD
avdmanager create avd -n shortdrama_zh -k "system-images;android-34;google_apis;arm64-v8a" -d "pixel_6"

# 英文环境 AVD
avdmanager create avd -n shortdrama_en -k "system-images;android-34;google_apis;arm64-v8a" -d "pixel_6"

# 低端设备 AVD（2GB RAM, Android 12）
avdmanager create avd -n shortdrama_low -k "system-images;android-31;google_apis;arm64-v8a" -d "pixel_4a"
```

### 5.2 启动与停止

```bash
# 启动模拟器（冷启动，不使用快照）
emulator -avd shortdrama_test -no-boot-anim -no-snapshot &

# 启动模拟器 + 指定端口（并行多台）
emulator -avd shortdrama_test -port 5554 -no-boot-anim &

# 启动模拟器 + 可写系统分区（需要 root）
emulator -avd shortdrama_test -writable-system -no-boot-anim &

# 完全静默启动（无窗口，纯后台）
emulator -avd shortdrama_test -no-window -no-audio -no-boot-anim &

# 启动后等待设备就绪（脚本化必备）
emulator -avd shortdrama_test -no-boot-anim &
adb wait-for-device
# 等待启动完成（sys.boot_completed=1）
while [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" != "1" ]; do
    echo "Waiting for emulator..."
    sleep 2
done
echo "Emulator ready"

# 停止模拟器
adb -s emulator-5554 emu kill

# 或杀掉所有模拟器进程
pkill -f "emulator.*-avd"
```

**常用启动参数**：

| 参数 | 用途 |
|------|------|
| `-no-boot-anim` | 跳过启动动画（加快启动） |
| `-no-audio` | 禁用音频输出 |
| `-no-window` | 无 GUI 窗口（CI/服务器） |
| `-no-snapshot` | 不使用快照，始终冷启动 |
| `-gpu host` | 使用宿主机 GPU 渲染（加速） |
| `-netdelay none` | 取消网络延迟模拟 |
| `-netspeed full` | 全速网络 |
| `-writable-system` | 可写系统分区 |
| `-port 5556` | 指定 ADB 端口（5554 起，每 +2 增加一个） |
| `-memory 4096` | 分配内存（MB） |
| `-read-only` | 只读系统分区 |

### 5.3 快照

快照可大幅加速模拟器启动（温启动 < 5 秒）。

```bash
# 启动模拟器后，保存当前状态为快照
adb -s emulator-5554 emu avd snapshot save clean_state

# 列出所有快照
adb emu avd snapshot list

# 从快照恢复启动（快于冷启动）
emulator -avd shortdrama_test -snapshot clean_state -no-boot-anim &

# 删除指定快照
adb emu avd snapshot delete clean_state

# 创建快照的推荐工作流：
# 1. 冷启动模拟器
emulator -avd shortdrama_test -no-boot-anim &
adb wait-for-device
# 2. 安装应用
adb install -r app/build/outputs/apk/debug/app-debug.apk
# 3. 完成首次启动配置（跳过引导页、登录等）
adb shell am start -n com.djs66256.short_drama/.MainActivity
# 4. 保存快照
adb emu avd snapshot save baseline

# 后续快速恢复：
emulator -avd shortdrama_test -snapshot baseline -no-boot-anim &
```

**快照最佳实践**：
- 快照名称使用语义化命名：`baseline`（基础环境）、`after_login`（登录后）、`video_loaded`（视频加载后）。
- 快照不宜过多（超过 5 个影响管理），定期清理不再使用的快照。
- CI 环境中使用快照可节省 30-60 秒的启动时间。
