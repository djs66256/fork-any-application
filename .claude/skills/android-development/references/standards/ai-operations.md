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

<!-- TODO: uiautomator dump、UI 树解析 -->

### 2.2 Espresso

<!-- TODO: Espresso 自动化测试方案 -->

---

## 3. 截图与视觉比对

<!-- TODO: 补充截图采集流程 -->

### 3.1 截图采集

<!-- TODO: 指定页面截图、全屏截图 -->

### 3.2 设计稿比对

<!-- TODO: 与 Figma/设计稿自动比对的流程 -->

---

## 4. 日志采集

<!-- TODO: 补充 Logcat 日志采集方案 -->

### 4.1 实时日志

<!-- TODO: logcat -v time 实时输出 -->

### 4.2 日志过滤

<!-- TODO: 按 TAG、级别、关键词过滤 -->

### 4.3 Bug Report

<!-- TODO: bugreport 导出与解析 -->

---

## 5. 模拟器管理

<!-- TODO: 补充 AVD 管理 -->

### 5.1 创建与配置

<!-- TODO: avdmanager 创建 AVD -->

### 5.2 启动与停止

<!-- TODO: emulator 命令行参数 -->

### 5.3 快照

<!-- TODO: 快照保存与恢复 -->
