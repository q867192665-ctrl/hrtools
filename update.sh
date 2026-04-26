#!/bin/bash

# ============================================================
# 人事管理系统 - Docker 更新脚本
# 宿主机: homeassistant (Linux)
# 容器名: hrtools
# ============================================================

set -e

echo "=========================================="
echo "人事管理系统 - Docker 更新"
echo "宿主机: homeassistant"
echo "容器名: hrtools"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}错误: Docker未运行或当前用户无权限${NC}"
    exit 1
fi

# 步骤1: 停止当前容器
echo -e "${YELLOW}[1/5] 停止当前容器 hrtools...${NC}"
docker stop hrtools
docker rm hrtools
echo -e "${GREEN}✓ 容器已停止${NC}"
echo ""

# 步骤2: 重新构建镜像
echo -e "${YELLOW}[2/5] 重新构建Docker镜像...${NC}"
docker-compose build --no-cache
echo -e "${GREEN}✓ 镜像构建完成${NC}"
echo ""

# 步骤3: 启动新容器
echo -e "${YELLOW}[3/5] 启动新容器 hrtools...${NC}"
docker-compose up -d
echo -e "${GREEN}✓ 容器已启动${NC}"
echo ""

# 步骤4: 等待服务启动
echo -e "${YELLOW}[4/5] 等待服务启动...${NC}"
sleep 5

# 步骤5: 检查服务状态
echo -e "${YELLOW}[5/5] 检查容器状态...${NC}"
if docker ps --filter "name=hrtools" --filter "status=running" | grep -q "hrtools"; then
    echo -e "${GREEN}✓ 容器 hrtools 运行正常${NC}"
    echo ""
    echo "=========================================="
    echo -e "${GREEN}更新完成！${NC}"
    echo "=========================================="
    echo ""
    echo "访问地址: http://homeassistant:32996"
    echo ""
    echo "查看日志: docker logs -f hrtools"
    echo "停止服务: docker stop hrtools"
    echo "重启服务: docker restart hrtools"
    echo ""
else
    echo -e "${RED}✗ 容器启动失败，请查看日志${NC}"
    echo ""
    echo "查看日志: docker logs hrtools"
    exit 1
fi
