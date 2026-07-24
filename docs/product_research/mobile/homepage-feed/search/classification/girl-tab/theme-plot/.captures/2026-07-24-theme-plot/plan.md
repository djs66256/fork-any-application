# 采集规划：红果 — 女生 Tab 主题情节标签组

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/search/classification/girl-tab/theme-plot |
| 采集范围 | 分类页女生 Tab 下“主题情节”标签组的可见结构、标签内容与返回定位 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：记录女生 Tab 下的主题情节标签组

- **操作**：进入分类页“女生”Tab，观察右侧中段“主题情节”标签组
- **命令**：复用 `homepage-feed/search/classification/girl-tab/.captures/2026-07-24-girl-tab` 中已验证的女生 Tab 样本
- **截图**：保存为 `assets/2026-07-24-step-01-theme-plot.png`
- **观察要点**：确认该路径是否为同页分组、记录主题情节可见标签与页面结构

*采集阶段回填：*
- **观察**：女生 Tab 下的“主题情节”并不是单独页面，而是同一长页中位于时代背景之后的第二个标签分组。当前样本可见标签包括“打脸虐渣 / 逆袭 / 马甲 / 女性成长 / 重生 / 穿越 / 系统 / 亲情 / 家庭伦理 / 奇幻爱情 / 闪婚 / 暗恋成真 / 古风言情 / 穿书 / 破镜重圆 / 追妻 / 现代言情 / 豪门恩怨 / 虐恋 / 古风权谋 / 年代爱情 / 娱乐圈 / 剧情 / 悬疑推理 / 喜剧 / 现言甜宠”。这表明 theme-plot 是女生内容池中的核心题材标签集合。
- **截图文件**：`assets/2026-07-24-step-01-theme-plot.png`

### 步骤 2：确认返回落点

- **操作**：复用女生 Tab 路径中已验证的返回样本，确认该分组所在层级的返回落点
- **命令**：复用 `homepage-feed/search/classification/girl-tab/.captures/2026-07-24-girl-tab/assets/2026-07-24-step-03-back-context.png`
- **截图**：保存为 `assets/2026-07-24-step-02-back-context.png`
- **观察要点**：确认返回是退出整个分类承接层，还是仅回到女生 Tab 其他分组

*采集阶段回填：*
- **观察**：从女生 Tab 所在分类承接层返回后，页面直接回到搜索承接页，而不是停留在分类页内部其他维度。这说明 theme-plot 作为女生 Tab 内部标签分组，仍属于搜索分类承接层内部状态。
- **截图文件**：`assets/2026-07-24-step-02-back-context.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-theme-plot.png` | 截图 | 步骤 1 | 女生 Tab 的主题情节标签组 |
| `assets/2026-07-24-step-01-theme-plot.xml` | XML | 步骤 1 | 女生 Tab 的主题情节标签组界面树 |
| `assets/2026-07-24-step-02-back-context.png` | 截图 | 步骤 2 | 返回后的搜索页 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 15:55:59 | 归档女生 Tab 主题情节样本 | `cp homepage-feed/search/classification/girl-tab/.captures/2026-07-24-girl-tab/assets/2026-07-24-step-02-girl-tab.png homepage-feed/search/classification/girl-tab/theme-plot/.captures/2026-07-24-theme-plot/assets/2026-07-24-step-01-theme-plot.png && cp homepage-feed/search/classification/girl-tab/.captures/2026-07-24-girl-tab/assets/2026-07-24-step-02-girl-tab.xml homepage-feed/search/classification/girl-tab/theme-plot/.captures/2026-07-24-theme-plot/assets/2026-07-24-step-01-theme-plot.xml` | 成功，归档女生 Tab 样本中的主题情节分组 |
| 15:55:59 | 归档返回样本 | `cp homepage-feed/search/classification/girl-tab/.captures/2026-07-24-girl-tab/assets/2026-07-24-step-03-back-context.png homepage-feed/search/classification/girl-tab/theme-plot/.captures/2026-07-24-theme-plot/assets/2026-07-24-step-02-back-context.png` | 成功，确认返回后直接回搜索承接页 |

## 异常记录

- 本轮未单独重新采集 theme-plot，因为父路径 `girl-tab` 的已验证样本已完整覆盖该分组的可见状态；因此直接复用同屏截图与 XML 拆分模块级文档。

## 执行状态

- [x] 步骤 1：记录女生 Tab 下的主题情节标签组
- [x] 步骤 2：确认返回落点
