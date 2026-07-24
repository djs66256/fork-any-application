# 本地开发环境

本目录包含用于本地开发的 Docker Compose 配置，提供 Redis 和 PostgreSQL（Supabase 兼容）服务。

## 包含服务

| 服务 | 端口 | 说明 |
|------|------|------|
| Redis | `6379` | 缓存 + 消息队列 |
| PostgreSQL | `5432` | 数据库（Supabase 兼容，含 pgcrypto、uuid-ossp 扩展） |
| Supabase Studio | `8000` | Web 管理面板（表管理、SQL 编辑器、API 文档） |
| Mailpit | `8025` | 本地邮件捕获（Web UI） |

## 快速开始

```bash
# 1. 复制环境变量
cp .env.example .env

# 2. 启动所有服务
docker compose up -d

# 3. 查看状态
docker compose ps

# 4. 打开 Supabase Studio
open http://localhost:8000
```

## 常用命令

```bash
# 停止服务
docker compose down

# 停止并清空数据（重新初始化）
docker compose down -v

# 查看日志
docker compose logs -f          # 全部
docker compose logs -f redis    # 指定服务

# 重启单个服务
docker compose restart redis

# 进入 PostgreSQL
docker compose exec postgres psql -U postgres

# 进入 Redis CLI
docker compose exec redis redis-cli
```

## 端口冲突

如果某些端口已被占用，编辑 `.env` 文件修改对应端口号。
