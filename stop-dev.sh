#!/usr/bin/env bash
# ============================================================
# 停止 backend + web 开发服务器（不影响 Docker）
# ============================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}正在查找并停止开发服务器...${NC}"

# 按端口杀掉占用 3000 (web) 和 3001 (backend) 的 next dev 进程
STOPPED=0

for PORT in 3000 3001; do
  PIDS=$(lsof -ti:$PORT 2>/dev/null || true)
  if [ -n "$PIDS" ]; then
    echo "$PIDS" | while read -r pid; do
      kill "$pid" 2>/dev/null && echo -e "${GREEN}✓${NC} 已停止端口 $PORT 上的进程 (PID: $pid)"
    done
    STOPPED=$((STOPPED + 1))
  else
    echo "  端口 $PORT 上没有运行中的服务"
  fi
done

if [ "$STOPPED" -eq 0 ]; then
  echo -e "${GREEN}没有运行中的开发服务器。${NC}"
else
  echo -e "${GREEN}开发服务器已全部停止。Docker 不受影响。${NC}"
fi
