#!/bin/bash

# ============================================================
# 人事管理系统 - 从 GitHub 更新登录页面到 Docker
# 宿主机: homeassistant (Linux)
# 容器名: hrtools
# GitHub: https://github.com/q867192665-ctrl/hrtools.git
# ============================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

REPO_URL="https://github.com/q867192665-ctrl/hrtools.git"
TEMP_DIR="/tmp/hrtools_update"

echo "=========================================="
echo "人事管理系统 - 从 GitHub 更新"
echo "容器名: hrtools"
echo "=========================================="
echo ""

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}错误: Docker未运行或当前用户无权限${NC}"
    exit 1
fi

# 步骤1: 检查容器状态
echo -e "${YELLOW}[1/4] 检查容器状态...${NC}"
if ! docker ps --filter "name=hrtools" --filter "status=running" | grep -q "hrtools"; then
    echo -e "${RED}错误: 容器 hrtools 未运行${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 容器运行正常${NC}"
echo ""

# 步骤2: 从 GitHub 下载最新代码
echo -e "${YELLOW}[2/4] 从 GitHub 下载最新代码...${NC}"
rm -rf "$TEMP_DIR"
git clone --depth 1 "$REPO_URL" "$TEMP_DIR"
echo -e "${GREEN}✓ 代码下载完成${NC}"
echo ""

# 步骤3: 复制登录页面到容器
echo -e "${YELLOW}[3/4] 复制登录页面到容器...${NC}"
docker cp "$TEMP_DIR/backend/templates/login.html" hrtools:/app/backend/templates/login.html
echo -e "${GREEN}✓ 文件已复制${NC}"
echo ""

# 步骤4: 重启容器
echo -e "${YELLOW}[4/4] 重启容器使更改生效...${NC}"
docker restart hrtools
sleep 3

# 清理临时文件
rm -rf "$TEMP_DIR"

# 检查结果
if docker ps --filter "name=hrtools" --filter "status=running" | grep -q "hrtools"; then
    echo -e "${GREEN}✓ 更新完成！${NC}"
    echo ""
    echo "=========================================="
    echo -e "${GREEN}登录页面已更新${NC}"
    echo "=========================================="
    echo ""
    echo "访问地址: http://homeassistant:32996"
    echo ""
    echo "查看日志: docker logs -f hrtools"
    echo "重启服务: docker restart hrtools"
else
    echo -e "${RED}✗ 容器重启失败${NC}"
    echo ""
    echo "查看日志: docker logs hrtools"
    exit 1
fi
