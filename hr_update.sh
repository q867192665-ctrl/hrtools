#!/bin/bash

# ============================================================
# 人事管理系统 - Docker 更新脚本
# 宿主机: homeassistant (Linux)
# 容器名: hrtools
# 
# 使用方法：
# 1. 将此脚本上传到homeassistant服务器
# 2. 修改 PROJECT_DIR 变量为实际项目路径
# 3. 执行: ./hr_update.sh
# ============================================================

set -e

# ========== 配置区（请根据实际情况修改）==========
# 项目目录路径（请修改为实际路径）
PROJECT_DIR="/config/hrtools"  # 常见路径：/config/hrtools 或 /root/22buckup 或 /opt/hrtools

# 容器名称
CONTAINER_NAME="hrtools"

# 服务端口
SERVICE_PORT="32996"
# ================================================

echo "=========================================="
echo "人事管理系统 - Docker 更新"
echo "宿主机: homeassistant"
echo "容器名: ${CONTAINER_NAME}"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}错误: Docker未运行或当前用户无权限${NC}"
    exit 1
fi

# 检查项目目录是否存在
if [ ! -d "${PROJECT_DIR}" ]; then
    echo -e "${RED}错误: 项目目录 ${PROJECT_DIR} 不存在${NC}"
    echo ""
    echo "请修改脚本中的 PROJECT_DIR 变量为实际项目路径"
    echo "常见路径："
    echo "  - /config/hrtools"
    echo "  - /root/22buckup"
    echo "  - /opt/hrtools"
    echo ""
    echo "使用以下命令查找项目文件："
    echo "  find / -name \"docker-compose.yml\" 2>/dev/null"
    exit 1
fi

# 进入项目目录
cd "${PROJECT_DIR}"

# 检查docker-compose.yml是否存在
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}错误: 目录 ${PROJECT_DIR} 中找不到 docker-compose.yml${NC}"
    exit 1
fi

# 步骤1: 停止当前容器
echo -e "${YELLOW}[1/5] 停止当前容器 ${CONTAINER_NAME}...${NC}"
docker stop ${CONTAINER_NAME} 2>/dev/null || true
docker rm ${CONTAINER_NAME} 2>/dev/null || true
echo -e "${GREEN}✓ 容器已停止${NC}"
echo ""

# 步骤2: 重新构建镜像
echo -e "${YELLOW}[2/5] 重新构建Docker镜像...${NC}"
docker-compose build --no-cache
echo -e "${GREEN}✓ 镜像构建完成${NC}"
echo ""

# 步骤3: 启动新容器
echo -e "${YELLOW}[3/5] 启动新容器 ${CONTAINER_NAME}...${NC}"
docker-compose up -d
echo -e "${GREEN}✓ 容器已启动${NC}"
echo ""

# 步骤4: 等待服务启动
echo -e "${YELLOW}[4/5] 等待服务启动...${NC}"
sleep 5

# 步骤5: 检查服务状态
echo -e "${YELLOW}[5/5] 检查容器状态...${NC}"
if docker ps --filter "name=${CONTAINER_NAME}" --filter "status=running" | grep -q "${CONTAINER_NAME}"; then
    echo -e "${GREEN}✓ 容器 ${CONTAINER_NAME} 运行正常${NC}"
    echo ""
    echo "=========================================="
    echo -e "${GREEN}更新完成！${NC}"
    echo "=========================================="
    echo ""
    echo "访问地址: http://homeassistant:${SERVICE_PORT}"
    echo ""
    echo "查看日志: docker logs -f ${CONTAINER_NAME}"
    echo "停止服务: docker stop ${CONTAINER_NAME}"
    echo "重启服务: docker restart ${CONTAINER_NAME}"
    echo ""
else
    echo -e "${RED}✗ 容器启动失败，请查看日志${NC}"
    echo ""
    echo "查看日志: docker logs ${CONTAINER_NAME}"
    exit 1
fi
