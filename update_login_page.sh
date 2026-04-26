#!/bin/bash

# ============================================================
# 人事管理系统 - 更新登录页面
# 宿主机: homeassistant (Linux)
# 容器名: hrtools
# ============================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}[1/3] 检查容器状态...${NC}"
if ! docker ps --filter "name=hrtools" --filter "status=running" | grep -q "hrtools"; then
    echo -e "${RED}错误: 容器 hrtools 未运行${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 容器运行正常${NC}"

echo -e "${YELLOW}[2/3] 复制登录页面到容器...${NC}"
docker cp "$(dirname "$0")/backend/templates/login.html" hrtools:/app/backend/templates/login.html
echo -e "${GREEN}✓ 文件已复制${NC}"

echo -e "${YELLOW}[3/3] 重启容器使更改生效...${NC}"
docker restart hrtools
sleep 3

if docker ps --filter "name=hrtools" --filter "status=running" | grep -q "hrtools"; then
    echo -e "${GREEN}✓ 更新完成！${NC}"
    echo ""
    echo "访问地址: http://homeassistant:32996"
else
    echo -e "${RED}✗ 容器重启失败${NC}"
    echo "查看日志: docker logs hrtools"
    exit 1
fi
