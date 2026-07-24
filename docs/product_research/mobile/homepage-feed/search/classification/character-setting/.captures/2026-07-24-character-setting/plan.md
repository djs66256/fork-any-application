# 采集规划：红果 — 分类页角色设定维度

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/search/classification/character-setting |
| 采集范围 | 进入分类页默认“全部”态，切换左侧“角色设定”，记录标签组结构与返回链路 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入分类页“角色设定”维度

- **操作**：从首页进入搜索页，点击“分类”，保持顶部“全部”态并点击左侧“角色设定”
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 926 264 && sleep 2 && adb shell input tap 118 540 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-01-character-setting.png`
- **观察要点**：确认顶部“全部”和左侧“角色设定”高亮，并记录右侧角色设定标签矩阵与同页结构

*采集阶段回填：*
- **观察**：点击左侧“角色设定”后，页面仍停留在分类中心同一页，顶部保持“全部”高亮，左侧以橙色高亮“角色设定”。右侧不是单独切换成新页面，而是继续以纵向长页方式保留“时代背景 / 主题情节 / 角色设定”三个分组，其中角色设定分组位于下部。当前可见标签包括“大女主 / 萌宝 / 小人物 / 神豪 / 强者回归 / 真假千金 / 欢喜冤家 / 强强联合 / 天下无敌 / 青梅竹马 / 王妃 / 女帝 / 龙王 / 皇后 / 替身 / 大叔 / 团宠”，说明默认“全部”态会混合展示女性人设、男性爽感身份与关系型角色模板。
- **截图文件**：`assets/2026-07-24-step-01-character-setting.png`

### 步骤 2：返回搜索页上文

- **操作**：从角色设定浏览态执行返回，确认回到上一层
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-02-back-context.png`
- **观察要点**：记录返回落点，确认角色设定维度仍属于分类承接层内部状态

*采集阶段回填：*
- **观察**：从分类页“角色设定”浏览态返回后，直接回到搜索承接页，而不是停留在分类页。这说明角色设定仍是分类中心内部锚点分组，而非独立页面。
- **截图文件**：`assets/2026-07-24-step-02-back-context.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-character-setting.png` | 截图 | 步骤 1 | 分类页角色设定维度 |
| `assets/2026-07-24-step-01-character-setting.xml` | XML | 步骤 1 | 分类页角色设定界面树 |
| `assets/2026-07-24-step-02-back-context.png` | 截图 | 步骤 2 | 返回后的搜索页 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 15:19:16 | 进入分类页角色设定 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 926 264 && sleep 2 && adb shell input tap 118 540 && sleep 2` | 成功，顶部“全部”和左侧“角色设定”高亮并显示角色设定标签组 |
| 15:19:20 | 返回搜索页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 成功，直接返回搜索承接页 |

## 异常记录

无异常

## 执行状态

- [x] 步骤 1：进入分类页“角色设定”维度
- [x] 步骤 2：返回搜索页上文
