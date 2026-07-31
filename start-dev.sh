#!/usr/bin/env bash
# ============================================================
# 一键启动 backend + web 测试服务
#
# 用途: 启动 Docker 基础设施（Supabase + Redis），
#       然后并行启动 backend 和 web 开发服务器。
#
# 使用: ./start-dev.sh
# 停止: Ctrl+C（自动清理 Docker 服务）
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"
WEB_DIR="$SCRIPT_DIR/web"
DOCKER_COMPOSE_FILE="$BACKEND_DIR/tests/docker-compose.yml"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

cleanup() {
  echo ""
  echo -e "${YELLOW}正在停止所有服务...${NC}"

  # 停止后台进程（backend + web dev server）
  if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    kill "$BACKEND_PID" 2>/dev/null
    echo -e "${GREEN}✓${NC} backend 开发服务器已停止"
  fi
  if [ -n "$WEB_PID" ] && kill -0 "$WEB_PID" 2>/dev/null; then
    kill "$WEB_PID" 2>/dev/null
    echo -e "${GREEN}✓${NC} web 开发服务器已停止"
  fi

  # 等待进程退出
  wait 2>/dev/null || true

  # echo -e "${YELLOW}正在停止 Docker 基础设施...${NC}"
  # docker compose -f "$DOCKER_COMPOSE_FILE" down
  # echo -e "${GREEN}✓${NC} Docker 基础设施已停止"
  # echo -e "${GREEN}所有服务已停止。${NC}"
  exit 0
}

# 注册 cleanup 回调
trap cleanup SIGINT SIGTERM

echo -e "${CYAN}============================================================${NC}"
echo -e "${CYAN}  ShortDrama 开发环境一键启动${NC}"
echo -e "${CYAN}============================================================${NC}"
echo ""

# ── 1. 检查依赖 ────────────────────────────────────────────
echo -e "${YELLOW}[1/4]${NC} 检查依赖..."

if ! command -v node &>/dev/null; then
  echo -e "${RED}✗${NC} 未找到 Node.js，请先安装 Node.js"
  exit 1
fi

if ! command -v docker &>/dev/null; then
  echo -e "${RED}✗${NC} 未找到 Docker，请先安装 Docker Desktop"
  exit 1
fi

if ! docker info &>/dev/null 2>&1; then
  echo -e "${RED}✗${NC} Docker 未运行中，请先启动 Docker Desktop"
  exit 1
fi

echo -e "${GREEN}✓${NC} Node.js $(node --version)"
echo -e "${GREEN}✓${NC} Docker 运行中"

# ── 2. 启动 Docker 基础设施 ──────────────────────────────────
echo ""
echo -e "${YELLOW}[2/4]${NC} 启动 Docker 基础设施（Supabase + Redis）..."

# 检查是否已经在运行
if docker compose -f "$DOCKER_COMPOSE_FILE" ps --status running 2>/dev/null | grep -q "fork-any"; then
  echo -e "${GREEN}✓${NC} Docker 服务已在运行中"
else
  docker compose -f "$DOCKER_COMPOSE_FILE" up -d
  echo -e "${YELLOW}  等待数据库就绪（约 15 秒）...${NC}"
  sleep 15
  echo -e "${GREEN}✓${NC} Docker 基础设施已启动"
  echo "   PostgreSQL:  localhost:5432"
  echo "   Supabase:    localhost:54321"
  echo "   Studio:      http://localhost:8000"
  echo "   Redis:       localhost:6379"
  echo "   Mailpit:     http://localhost:8025"
fi

# ── 3. 安装依赖 ──────────────────────────────────────────────
echo ""
echo -e "${YELLOW}[3/4]${NC} 检查并安装依赖..."

if [ ! -d "$BACKEND_DIR/node_modules" ]; then
  echo "  安装 backend 依赖..."
  (cd "$BACKEND_DIR" && npm install)
fi
if [ ! -d "$WEB_DIR/node_modules" ]; then
  echo "  安装 web 依赖..."
  (cd "$WEB_DIR" && npm install)
fi

echo -e "${GREEN}✓${NC} 依赖就绪"

# ── 4. 启动开发服务器 ────────────────────────────────────────
echo ""
echo -e "${YELLOW}[4/4]${NC} 启动开发服务器..."
echo ""

# 启动 backend
(cd "$BACKEND_DIR" && npm run dev) &
BACKEND_PID=$!
echo -e "${GREEN}✓${NC} backend 开发服务器启动中... (PID: $BACKEND_PID, 端口: 3001)"

# 启动 web
(cd "$WEB_DIR" && npm run dev) &
WEB_PID=$!
echo -e "${GREEN}✓${NC} web 开发服务器启动中... (PID: $WEB_PID, 端口: 3000)"

echo ""
echo -e "${CYAN}============================================================${NC}"
echo -e "${CYAN}  所有服务已启动！${NC}"
echo ""
echo "   Backend API:  http://localhost:3001"
echo "   Web 前端:     http://localhost:3000"
echo "   Supabase:     http://localhost:8000"
echo "   Mailpit:      http://localhost:8025"
echo ""
echo "   按 ${YELLOW}Ctrl+C${NC} 停止所有服务"
echo -e "${CYAN}============================================================${NC}"

# 等待任意子进程退出
wait -n 2>/dev/null || true

# 如果有进程异常退出，触发 cleanup
cleanup
