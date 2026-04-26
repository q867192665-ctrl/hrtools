#!/bin/bash

# ============================================================
# homeassistant 一键更新脚本
# 用途：从GitHub拉取最新代码并重新构建Docker容器
# 容器名: hrtools
# ============================================================

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "=========================================="
echo -e "${BLUE}人事管理系统 - 一键更新${NC}"
echo "宿主机: homeassistant"
echo "容器名: hrtools"
echo "=========================================="
echo ""

# 检查Docker
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}错误: Docker未运行${NC}"
    exit 1
fi

# 检查git
if ! command -v git &> /dev/null; then
    echo -e "${RED}错误: 未安装git${NC}"
    echo "请先安装: apk add git"
    exit 1
fi

# 配置区
WORK_DIR="/tmp/hrtools_build"
REPO_URL="https://github.com/q867192665-ctrl/hrtools.git"
CONTAINER_NAME="hrtools"
PORT="32996"

# 步骤1: 清理旧目录
echo -e "${YELLOW}[1/6] 准备工作目录...${NC}"
rm -rf "${WORK_DIR}"
mkdir -p "${WORK_DIR}"
cd "${WORK_DIR}"
echo -e "${GREEN}✓ 工作目录: ${WORK_DIR}${NC}"
echo ""

# 步骤2: 拉取最新代码
echo -e "${YELLOW}[2/6] 从GitHub拉取最新代码...${NC}"
git clone "${REPO_URL}" .
echo -e "${GREEN}✓ 代码拉取完成${NC}"
echo ""

# 步骤3: 停止旧容器
echo -e "${YELLOW}[3/6] 停止旧容器 ${CONTAINER_NAME}...${NC}"
docker stop ${CONTAINER_NAME} 2>/dev/null || true
docker rm ${CONTAINER_NAME} 2>/dev/null || true
echo -e "${GREEN}✓ 旧容器已清理${NC}"
echo ""

# 步骤4: 构建新镜像
echo -e "${YELLOW}[4/6] 构建新Docker镜像...${NC}"
echo "(这可能需要几分钟，请耐心等待)"
docker-compose build --no-cache
echo -e "${GREEN}✓ 镜像构建完成${NC}"
echo ""

# 步骤5: 启动新容器
echo -e "${YELLOW}[5/6] 启动新容器...${NC}"
docker-compose up -d
echo -e "${GREEN}✓ 容器已启动${NC}"
echo ""

# 步骤6: 检查状态
echo -e "${YELLOW}[6/6] 检查容器状态...${NC}"
sleep 5
if docker ps --filter "name=${CONTAINER_NAME}" --filter "status=running" | grep -q "${CONTAINER_NAME}"; then
    echo -e "${GREEN}✓ 容器运行正常${NC}"
    echo ""
    echo "=========================================="
    echo -e "${GREEN}更新完成！${NC}"
    echo "=========================================="
    echo ""
    echo "访问地址: http://homeassistant:${PORT}"
    echo ""
    echo "常用命令:"
    echo "  查看日志: docker logs -f ${CONTAINER_NAME}"
    echo "  重启服务: docker restart ${CONTAINER_NAME}"
    echo "  停止服务: docker stop ${CONTAINER_NAME}"
    echo ""
else
    echo -e "${RED}✗ 容器启动失败${NC}"
    echo ""
    echo "查看日志: docker logs ${CONTAINER_NAME}"
    exit 1
fi

# 清理临时目录
echo "清理临时文件..."
cd /
rm -rf "${WORK_DIR}"
echo "完成！"
