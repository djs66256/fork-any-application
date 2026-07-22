# 文档工程规范

竞品业务功能文档的目录结构、命名、模板、索引与写作标准。Review 规范见 `references/review.md`。

## 1. 三种文档的定位

| 文档类型 | 定位 | 目录 | 更新方式 |
|---------|------|------|---------|
| **功能文档** | 完整的竞品业务功能文档，供产品团队持续参考 | `<频道>/<功能模块>/<功能>.md` | 每次采集后增量更新 |
| **采集文档** | 操作计划兼实录。阶段 1 预制操作序列，阶段 2 subagent 回填观察和日志 | `captures/<日期>-<描述>/capture.md` | 阶段 1 预制（操作序列），阶段 2 subagent 回填（观察/日志/产物） |
| **分析文档** | 基于采集产物的结构化分析结果 | `captures/<日期>-<描述>/analysis.md` | 每次采集后新建 |

功能文档是**合成物**，采集文档是**计划 + 实录**，分析文档是**中间产物**。功能文档中的每一条结论都应能从采集文档和分析文档中找到对应佐证。

## 2. 目录结构

```
docs/product_research/
├── index.md                              # 总索引（频道列表 + captures + revisions）
├── captures/                             # 采集文档目录（计划 + 实录）
│   └── <YYYY-MM-DD>-<描述>/
│       ├── capture.md                    # 采集文档（按 capture-template.md 模板）
│       ├── analysis.md                   # 分析文档（按 analysis-template.md 模板）
│       └── assets/                       # 本次采集的截图与录屏
├── revisions/                            # 修订历史
│   └── <YYYY-MM-DD>-<描述>.md
├── <频道>/                               # mobile / web
│   ├── index.md                          # 频道索引（功能模块列表）
│   └── <功能模块>/                        # kebab-case，如 homepage-feed
│       ├── index.md                      # 模块索引（功能文档 + 子项列表）
│       └── <功能>.md                      # 功能文档（完整合成文档，增量更新）
```

> `captures/` 和 `revisions/` 不需要 `index.md`。

## 3. 频道与采集方案

每个频道对应一种竞品运行环境，频道内部可配置多种采集方案：

| 频道 | 采集方案 | 命令参考 | 状态 |
|------|---------|---------|------|
| mobile | ADB（Android 模拟器） | `references/mobile-adb.md` | ✅ 已启用 |
| mobile | iOS（后续补充） | — | 🔜 规划中 |
| web | Playwright / Puppeteer | — | 🔜 规划中 |

频道方案通过 `references/<频道>-<方案>.md` 文件引用，采集 subagent 按需加载对应命令参考。

## 4. 命名约定

### 功能文档

```
<频道>/<功能模块>/<功能>.md
```

固定名称。每次采集后增量更新同一文件。

### 采集文档

```
captures/<YYYY-MM-DD>-<描述>/capture.md
```

每次采集新建一个子目录。阶段 1 按 `assets/capture-template.md` 模板预制操作序列，阶段 2 由采集 subagent 读取并回填观察、日志、产物清单。

> 日期为固定 10 字符 `YYYY-MM-DD`，之后的第一个 `-` 到目录名末尾为描述部分。描述中避免使用纯数字前缀以免与日期混淆（如 `5g-test` 建议改为 `network-5g-test`）。

截图/录屏放在同目录的 `assets/` 中，分析文档为同目录的 `analysis.md`。

### 修订历史

```
revisions/<YYYY-MM-DD>-<描述>.md
```

每次触发 skill 并修改文档后新建。内容包含采集文档地址和所有修改文件的简介。

### 功能模块目录

kebab-case：

| 功能 | 目录名 |
|------|--------|
| 首页推荐流 | `homepage-feed` |
| 播放器 | `video-player` |
| 搜索 | `search` |
| 登录注册 | `auth` |
| 个人中心 | `user-profile` |
| 评论互动 | `comments` |
| 分享 | `share` |
| 付费/会员 | `subscription` |

## 5. 索引规范

所有 `index.md` 只保留当前目录的条目索引，格式：

```markdown
- [名称](路径)
```

**根 `index.md`**：

```markdown
- [captures](captures/)
- [revisions](revisions/)
- [mobile](mobile/index.md)
```

**频道 `index.md`**：

```markdown
- [video-player](video-player/index.md)
- [homepage-feed](homepage-feed/index.md)
```

**模块 `index.md`**：

```markdown
- [homepage-feed](homepage-feed.md)
```

## 6. 模板引用

- 功能文档模板：`assets/doc-template.md`
- 采集文档模板：`assets/capture-template.md`
- 分析文档模板：`assets/analysis-template.md`
- 采集 subagent 模板：`references/capture-subagent.md`
- 分析 subagent 模板：`references/analysis.md`
- 成文 subagent 模板：`references/compilation-subagent.md`
- Review subagent 模板：`references/review.md`

## 7. 修订历史内容规范

```markdown
# 修订：{{日期}} — {{简短描述}}

## 采集文档

- [{{日期}}-{{描述}}](../captures/{{日期}}-{{描述}}/capture.md)

## 修改内容

### {{文档路径}}
{{修改内容简介}}
```

## 8. 交叉引用

功能文档引用采集文档中的截图：

```markdown
![冷启动首页](../../captures/2026-07-22-coldstart/assets/step-01-home.png)
```

## 9. 写作规范

- **事实观察**与**主观推断**必须明确区分
- 推断类结论标注 `[推断]`
- UI 描述尽可能数值化（dp、sp、颜色值等），无法精确识别的标注「约」
- 分析阶段额外规则见 `references/analysis.md`
- 成文阶段额外规则见 `references/compilation-subagent.md`
