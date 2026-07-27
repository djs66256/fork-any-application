# 扁平 UI 规范

## 设计原则

1. **简单扁平**：无阴影、无渐变、无动画/过渡特效，保持静态清晰
2. **内容结构优先**：信息架构清晰，导航和内容区分明
3. **一致性**：复用 Web 端 `tokens.css` 中的 CSS 自定义属性

## 色彩方案

| 用途 | CSS 变量 | 色值 |
|------|---------|------|
| 主色 | `--color-primary` | #2563EB (blue-600) |
| 主色 hover | `--color-primary-hover` | #1D4ED8 (blue-700) |
| 背景 | `--color-bg` | #F9FAFB (gray-50) |
| 卡片/表格背景 | `--color-surface` | #FFFFFF |
| 边框 | `--color-border` | #E5E7EB (gray-200) |
| 文字主色 | `--color-text` | #111827 (gray-900) |
| 文字次色 | `--color-text-secondary` | #6B7280 (gray-500) |
| 危险色 | `--color-danger` | #DC2626 (red-600) |

## 布局规范

```
┌──────────────────────────────────────────────────────────────┐
│  Header（高度 56px）                                          │
│  Logo/标题                  用户邮箱 + 退出                     │
├────────────┬─────────────────────────────────────────────────┤
│ 导航 (240px)│  内容区（padding: 24px）                          │
│            │                                                  │
│  仪表盘     │  ┌──────────┐ ┌──────────┐ ┌──────────┐        │
│  短剧管理   │  │ 统计卡片   │ │ 统计卡片   │ │ 统计卡片   │        │
│  用户管理   │  └──────────┘ └──────────┘ └──────────┘        │
│            │                                                  │
│            │  ┌──────────────────────────────────────────┐   │
│            │  │  表格 / 表单 区域                           │   │
│            │  └──────────────────────────────────────────┘   │
└────────────┴─────────────────────────────────────────────────┘
```

- 左侧导航固定宽度 240px
- 内容区留白 24px
- 页面最大宽度 1200px（内容居中，非全宽）

## 组件风格

### 按钮

| 类型 | 样式 | 用途 |
|------|------|------|
| 主按钮 | `background: var(--color-primary); color: #fff; border: none; border-radius: 6px; padding: 8px 16px;` | 主要操作（新建、保存、登录） |
| 次按钮 | `background: #fff; color: var(--color-text); border: 1px solid var(--color-border); border-radius: 6px; padding: 8px 16px;` | 次要操作（取消、返回） |
| 危险按钮 | `background: transparent; color: var(--color-danger); border: none;` | 删除操作 |
| 文字链接 | `color: var(--color-primary); text-decoration: none;` | 表格操作列（编辑、剧集） |

### 卡片

```
border: 1px solid var(--color-border);
border-radius: 8px;
background: var(--color-surface);
padding: 20px;
```

### 表格

```
width: 100%;
border-collapse: collapse;

th: 文字次色、12px、左对齐、底部边框
td: 14px、左对齐、底部边框 1px solid var(--color-border)
tr:hover: background: var(--color-bg)
```

- 操作列靠右对齐
- 分页器位于表格下方居右

### 表单

- 标签 + 输入框垂直排列
- 输入框：`border: 1px solid var(--color-border); border-radius: 6px; padding: 8px 12px;`
- 错误提示：红色文字，位于字段下方
- 表单宽度最大 480px（不横跨整个内容区）

### 统计卡片

```
border: 1px solid var(--color-border);
border-radius: 8px;
background: var(--color-surface);
padding: 20px;
text-align: center;

数字：28px/粗体
标签：14px/文字次色
```

## 状态展示

### 加载态
- 表格：骨架/占位文字「加载中...」
- 仪表盘卡片：占位文字「加载中...」

### 空态
- 居中显示「暂无数据」+ 新建按钮（如适用）
- 短剧/剧集空态：「暂无短剧，点击新建短剧开始」
- 用户空态：「暂无用户」

### 错误态
- 表格区域显示错误提示 + 重试按钮
- 仪表盘卡片区域显示错误提示 + 重试按钮
- 弹窗/Toast 提示操作失败原因

## 不使用

- ❌ 阴影（box-shadow）
- ❌ 渐变（gradient）
- ❌ 动画/过渡（animation/transition）
- ❌ 圆角过大的元素（border-radius > 8px）
- ❌ Tailwind CSS（使用 CSS Modules + tokens.css）
- ❌ 图标库（管理平台不需要图标）
