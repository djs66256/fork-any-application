# Monorepo 顶层结构搭建

在 PRD 撰写前，需要先初始化 monorepo 的顶层目录结构。

## 产物清单

| 产物 | 职责 |
|------|------|
| `CLAUDE.md` | 全局规则、目录职责、跨端约束、协作约定 |
| `PRODUCT.md` | 产品信息（名称、简介、竞品、技术标识） |
| `.gitignore` | 多端忽略规则（node_modules、build 产物、.env 等） |
| `README.md` | 项目简介、快速开始、目录导航 |
| 子目录 | `android/`、`ios/`、`web/`、`backend/`、`wiki/`、`docs/`、`.github/workflows/` |

## CLAUDE.md 模板

使用 `assets/claude-md-root-template.md`。

核心内容：
- 项目定位（fork 现有应用并推进落地的 harness 工程）
- 目录职责说明
- 协作约定（子目录规则优先于根目录规则）
- 开发约束（RESTful API、禁止硬编码常量、第三方库引入审批）
- 文档约定
- Git 规范
- Skill 开发规范
- 产品信息引用约定

## .gitignore 模板

使用 `assets/gitignore-template.md`。

覆盖：
- `node_modules/`、`.next/`
- iOS: `build/`、`*.xcworkspace/`、`DerivedData/`
- Android: `.gradle/`、`build/`、`local.properties`、`*.jks`、`*.apk`、`*.aab`
- IDE: `.idea/`、`*.iml`
- 环境: `.env`、`.env.local`
- Workspace: `.claude/worktrees/`

## 约束

- 空目录通过 `.gitkeep` 确保被 git 跟踪
- README.md 必须包含"快速开始"章节，让新加入者能在 30 分钟内启动任意一端
