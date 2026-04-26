#!/bin/bash

# ============================================================
# 直接更新容器内的Python代码（无需重新构建镜像）
# 容器名: hrtools
# ============================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "=========================================="
echo "直接更新容器内代码"
echo "容器名: hrtools"
echo "=========================================="
echo ""

CONTAINER_NAME="hrtools"
TEMP_DIR="/tmp/hrtools_update"

# 检查容器是否运行
if ! docker ps | grep -q "${CONTAINER_NAME}"; then
    echo -e "${RED}错误: 容器 ${CONTAINER_NAME} 未运行${NC}"
    exit 1
fi

# 1. 创建临时目录
echo -e "${YELLOW}[1/4] 准备更新文件...${NC}"
rm -rf "${TEMP_DIR}"
mkdir -p "${TEMP_DIR}"

# 从GitHub拉取最新的app.py
cd "${TEMP_DIR}"
git clone --depth 1 https://github.com/q867192665-ctrl/hrtools.git . 2>/dev/null || {
    echo -e "${RED}错误: 无法从GitHub拉取代码${NC}"
    echo "请检查网络连接或git是否安装"
    exit 1
}
echo -e "${GREEN}✓ 代码已拉取${NC}"
echo ""

# 2. 备份容器内的原文件
echo -e "${YELLOW}[2/4] 备份容器内原文件...${NC}"
docker exec ${CONTAINER_NAME} cp /app/backend/app.py /app/backend/app.py.backup.$(date +%Y%m%d_%H%M%S)
echo -e "${GREEN}✓ 备份完成${NC}"
echo ""

# 3. 复制新文件到容器
echo -e "${YELLOW}[3/4] 更新容器内代码...${NC}"
docker cp backend/app.py ${CONTAINER_NAME}:/app/backend/app.py
echo -e "${GREEN}✓ 代码已更新${NC}"
echo ""

# 4. 重启容器内的服务
echo -e "${YELLOW}[4/4] 重启服务...${NC}"
docker restart ${CONTAINER_NAME}
sleep 3
echo -e "${GREEN}✓ 服务已重启${NC}"
echo ""

# 检查状态
if docker ps | grep -q "${CONTAINER_NAME}"; then
    echo "=========================================="
    echo -e "${GREEN}更新完成！${NC}"
    echo "=========================================="
    echo ""
    echo "访问地址: http://homeassistant:32996"
    echo ""
    echo "查看日志: docker logs -f ${CONTAINER_NAME}"
    echo ""
else
    echo -e "${RED}✗ 容器启动失败${NC}"
    echo "查看日志: docker logs ${CONTAINER_NAME}"
    exit 1
fi

# 清理
rm -rf "${TEMP_DIR}"
