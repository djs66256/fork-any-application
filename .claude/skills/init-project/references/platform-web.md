# Web 初始化规范

## 技术栈

| 组件 | 选型 |
|------|------|
| 运行时 | Node.js ≥ 20 |
| 框架 | Next.js 16 App Router + React 19 |
| 语言 | TypeScript 5.x |
| 数据校验 | Zod ≥ 4 |
| 样式 | CSS Modules + CSS Custom Properties |
| 状态管理 | 初始化: RSC + client state；后续: TanStack Query + Zustand |
| 测试 | Vitest + Testing Library |

## 标准目录结构

```
web/
├── CLAUDE.md                      # 前端开发规范
├── package.json
├── tsconfig.json
├── .env.example
├── next.config.ts
├── src/
│   ├── app/                       # Page 层 — 文件系统路由
│   │   ├── layout.tsx             # 根布局（metadata + 全局 Providers）
│   │   ├── page.tsx               # 首页（应用名 + 版本号 + 环境标识）
│   │   ├── play/
│   │   │   └── [id]/
│   │   │       └── page.tsx       # 播放页占位
│   │   └── detail/
│   │       └── [id]/
│   │           └── page.tsx       # 详情页占位
│   ├── features/                  # Feature 层 — 按业务域独立目录
│   │   ├── home/
│   │   │   ├── components/
│   │   │   │   └── HomeScreen.tsx
│   │   │   └── index.ts
│   │   ├── player/                # 骨架
│   │   │   ├── components/
│   │   │   ├── hooks/
│   │   │   └── index.ts
│   │   └── drama-detail/          # 骨架
│   │       ├── components/
│   │       └── index.ts
│   ├── components/                # Shared UI 层
│   │   └── ui/
│   │       ├── Button.tsx
│   │       ├── Card.tsx
│   │       └── index.ts
│   ├── lib/                       # Core 层
│   │   ├── api-client.ts          # fetch wrapper（base URL + 错误处理 + 请求/响应拦截）
│   │   ├── config.ts              # NEXT_PUBLIC_* 环境变量
│   │   ├── schemas.ts             # Zod Schema（与 Backend 对齐）
│   │   └── types.ts               # 共享 TypeScript 类型
│   └── styles/                    # Design System 层
│       ├── globals.css            # CSS reset + 全局样式
│       └── tokens.css             # CSS 自定义属性（颜色、间距、字体、阴影）
└── README.md
```

## 路由设计

| 路径 | 页面 | 渲染策略 |
|------|------|---------|
| `/` | 首页（应用名 + 版本号 + 环境标识 + 路由导航） | SSR |
| `/play/[id]` | 播放页占位 | 默认 SSR（后续可改为 CSR） |
| `/detail/[id]` | 剧集详情页占位 | 默认 SSR（后续可改为 CSR） |

## API 客户端

`lib/api-client.ts` 封装 fetch，统一处理：
- Base URL（`NEXT_PUBLIC_API_URL` 环境变量，默认 `http://localhost:3001`）
- 请求头（Content-Type、Authorization）
- 错误分类（网络错误 vs API 错误）
- 响应 Zod 校验

## 端口策略

- Web 默认 3000
- Backend 默认 3001
- 避免同时开发时端口冲突

## 关键约束

- Feature 间禁止直接引用（通过 `lib/types.ts` 和 `lib/schemas.ts` 共享）
- Page 只做组合（import Feature 组件 + 传递 props）
- 所有 HTTP 请求通过 `lib/api-client.ts` 统一出口
- Schema 以 Backend 为准（`web/src/lib/schemas.ts` 手动对齐）
- 环境变量使用 `NEXT_PUBLIC_*` 前缀（客户端可访问）
- 禁止硬编码产品名/URL/Token
