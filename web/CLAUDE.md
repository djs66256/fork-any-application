## 端说明

Web 端代码与说明统一维护在当前目录。
如无额外说明，当前目录仅承载 Web 前端相关实现，不处理移动端或后端逻辑。

## 架构

采用五层架构（Page → Feature → Shared UI → Core → Design System），职责如下：

| 层级 | 目录 | 职责 |
|------|------|------|
| Page | `src/app/` | Next.js App Router 页面路由，Server Components 优先 |
| Feature | `src/features/` | 页面级业务组件（HomeScreen、PlayerScreen、DramaDetailScreen） |
| Shared UI | `src/components/ui/` | 通用 UI 组件（Button、Card、Container），不含业务逻辑 |
| Core | `src/lib/` | 核心工具库（config、schemas、api-client、types） |
| Design System | `src/styles/` | CSS tokens（颜色、间距、圆角、字体）和全局样式 |

约束：
- Page 层只做路由委托，不包含业务 UI 和状态逻辑
- Feature 层不直接发起网络请求，通过 Core 层封装调用
- Shared UI 组件不包含业务逻辑和副作用
- 数据输入输出使用 Zod 做结构约束与校验

## 技术栈

- Next.js 16 + React 19
- TypeScript
- Zod 数据校验
- CSS Modules + CSS 自定义属性（tokens）
- Vitest + Testing Library 测试

## 命令约定

| 命令 | 用途 |
|------|------|
| `npm run dev` | 开发服务器（Turbopack） |
| `npm run build` | 生产构建 |
| `npm run start` | 启动生产服务器 |
| `npm run lint` | ESLint 代码检查 |
| `npm test` | 运行全量测试（Vitest） |
| `npm run test:watch` | 测试监听模式 |

## 测试要求

- 测试框架：Vitest + Testing Library
- 组件测试覆盖关键 UI 渲染、交互和数据展示
- `schemas.ts` 和 `api-client.ts` 的测试必须覆盖合法/非法输入和错误路径
- 涉及业务逻辑、状态转换、数据校验的改动应补充对应测试

## 目录结构

```
web/
├── src/
│   ├── app/              # Page 层：Next.js App Router 路由页面
│   │   ├── layout.tsx
│   │   ├── page.tsx
│   │   ├── loading.tsx
│   │   ├── error.tsx
│   │   ├── not-found.tsx
│   │   ├── play/[id]/page.tsx
│   │   └── detail/[id]/page.tsx
│   ├── features/         # Feature 层：页面级业务组件
│   │   ├── home/
│   │   ├── player/
│   │   └── drama-detail/
│   ├── components/       # Shared UI 层：通用 UI 组件
│   │   └── ui/
│   ├── lib/              # Core 层：核心工具库
│   │   ├── config.ts
│   │   ├── schemas.ts
│   │   ├── types.ts
│   │   └── api-client.ts
│   └── styles/           # Design System 层
│       ├── tokens.css
│       └── globals.css
├── tests/
│   └── setup.ts          # Testing Library 初始化
├── vitest.config.ts
└── tsconfig.json
```

## 开发约定

- 仅修改 `web/` 目录下的文件
- SSR-first 策略，Server Components 优先
- API 对接遵循仓库根目录中的 RESTful 约束
- 禁止硬编码环境地址、token、接口前缀或其他环境相关常量
- API 调用通过 Core 层 `api-client.ts` 封装，base URL 从 `NEXT_PUBLIC_API_URL` 环境变量读取
