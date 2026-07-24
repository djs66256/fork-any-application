# 文档工程规范

竞品业务功能文档的目录结构、命名、模板、索引、写作标准和 PROGRESS.md 规范。

## 1. 三种文档的定位

| 文档类型 | 定位 | 目录 | 更新方式 |
|---------|------|------|---------|
| **功能文档** | 完整的竞品业务功能文档，供产品团队持续参考 | `<频道>/<页面>/<页面>.md` | 每次采集后增量更新 |
| **plan.md** | 采集规划兼实录。规划阶段预制操作序列，采集阶段回填观察和日志 | `<页面>/.captures/<日期>-<name>/plan.md` | 规划阶段预制，采集阶段回填 |
| **分析文档** | 基于采集产物的结构化分析结果 | `<页面>/.captures/<日期>-<name>/analysis.md` | 每次采集后新建 |

功能文档是**合成物**，plan.md 是**计划 + 实录**，分析文档是**中间产物**。功能文档中的每一条结论都应能从 plan.md 和分析文档中找到对应佐证。

## 2. 目录结构

频道下的目录分为两种类型：
- **页面型（page）**：按 UI 页面结构组织，如 `homepage-feed/`、`user-profile/`。可以有子页面层级。
- **功能型（func）**：按业务逻辑功能组织，如 `auth/`（登录注册流程）、`payment/`（支付流程）。这类功能通常是跨页面的完整业务链路。

两种类型拥有相同的内部结构。

```
docs/product_research/
├── index.md                              # 总索引（频道列表）
├── <频道>/                               # mobile / web
│   ├── index.md                          # 频道索引（页面/功能列表）
│   ├── PROGRESS.md                       # 频道调研进度（按路径追踪）
│   ├── <page-a>/                         # 页面型：具体页面，kebab-case
│   │   ├── .captures/                    # 采集文档（不进入业务逻辑索引）
│   │   │   └── <YYYY-MM-DD>-<name>/
│   │   │       ├── assets/               # 截图与录屏（png + mp4）
│   │   │       ├── plan.md               # 采集规划和执行进展
│   │   │       └── analysis.md           # 结构化分析结果
│   │   ├── index.md                      # 页面/功能索引
│   │   ├── <page-a>.md                   # 竞品业务功能文档（增量更新）
│   │   └── <page-a-sub>/                 # 层次子页面（如有）
│   │       ├── .captures/
│   │       ├── index.md
│   │       └── <page-a-sub>.md
│   └── <func-a>/                         # 功能型：业务逻辑功能，kebab-case
│       ├── .captures/
│       │   └── <YYYY-MM-DD>-<name>/
│       │       ├── assets/
│       │       ├── plan.md
│       │       └── analysis.md
│       ├── index.md
│       └── <func-a>.md                   # 竞品业务功能文档
```

**示例：**
```
mobile/
├── index.md
├── PROGRESS.md
├── homepage-feed/          # 页面型
│   ├── .captures/
│   ├── index.md
│   ├── homepage-feed.md
│   └── comments/           # 页面型的子页面
├── user-profile/           # 页面型
├── auth/                   # 功能型：登录注册流程
│   ├── .captures/
│   │   └── 2026-07-24-phone-login/
│   │       ├── assets/
│   │       ├── plan.md
│   │       └── analysis.md
│   ├── index.md
│   └── auth.md
└── payment/                # 功能型：支付流程
```

> `.captures/` 以 `.` 开头，表示该目录不进入业务逻辑索引范围，仅为采集过程的中间产物存放。
> `index.md` 中不应包含 `.captures/` 的引用。

## 3. 频道与采集方案

每个频道对应一种竞品运行环境，频道内部可配置多种采集方案：

| 频道 | 采集方案 | 命令参考 | 状态 |
|------|---------|---------|------|
| mobile | ADB（Android 模拟器） | `references/mobile-adb.md` | ✅ 已启用 |
| mobile | iOS（后续补充） | — | 🔜 规划中 |
| web | Playwright / Puppeteer | — | 🔜 规划中 |

## 4. 命名约定

### 功能文档

```
<频道>/<页面>/<页面>.md
```

固定名称。每次采集后增量更新同一文件。

### 采集文档

```
<页面>/.captures/<YYYY-MM-DD>-<name>/
├── plan.md
├── analysis.md
└── assets/
    └── *.png|*.mp4
```

每次采集新建一个子目录。规划阶段按 `assets/plan-template.md` 模板预制 plan.md，采集阶段回填观察、日志、产物清单。

> 日期为固定 10 字符 `YYYY-MM-DD`。`<name>` 为简短描述（如 `homepage-tab`、`core-chain`、`search-page`），kebab-case。

截图/录屏放在同目录 `assets/` 中，分析文档为同目录 `analysis.md`。

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
| 商城 | `mall` |
| 赚取中心 | `earn-center` |
| 剧场 | `theater` |

## 5. 索引规范

所有 `index.md` 只保留当前目录的条目索引，格式：`- [名称](路径)`

**根 `index.md`**：
```markdown
- [mobile](mobile/index.md)
- [web](web/index.md)
```

**频道 `index.md`**：
```markdown
- [homepage-feed](homepage-feed/index.md)
- [user-profile](user-profile/index.md)
- [earn-center](earn-center/index.md)
```

**页面 `index.md`**：
```markdown
- [homepage-feed](homepage-feed.md)
- [comments](comments/index.md)
```

## 6. PROGRESS.md 规范

每个频道一个 PROGRESS.md，放在频道根目录下。模板和字段说明见 `assets/progress-template.md`。

维护规则：
- 规划阶段：用户指定的入口写入第一行
- 验收阶段：每轮完成的路径标记为 ✅，新发现的路径追加到末尾
- 全部完成后提示用户，由用户决定是否继续新增入口

## 7. 模板引用

- plan.md 模板：`assets/plan-template.md`
- 分析文档模板：`assets/analysis-template.md`
- 功能文档模板：`assets/doc-template.md`
- PROGRESS.md 模板：`assets/progress-template.md`

## 8. 截图引用规范

功能文档引用同页面 `.captures/` 中的截图：

```markdown
![截图说明](.captures/<日期>-<name>/assets/<文件名>.png)
*图：<说明> | 采集于 <日期>*
```

plan.md 和 analysis.md 内部引用同目录 `assets/` 下的文件：

```markdown
![截图](assets/<文件名>.png)
```

## 9. 写作规范

- **事实观察**与**主观推断**必须明确区分
- 推断类结论标注 `[推断]`
- UI 描述尽可能数值化（dp、sp、颜色值等），无法精确识别的标注「约」
- 不适用当前功能文档的章节标注「本章节不适用」，不要删除
- 增量更新时保留已有内容，仅追加/修订相关章节
- 分析阶段额外规则见 `references/analysis.md`
