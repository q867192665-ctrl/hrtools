#!/bin/bash
# ============================================================
# 工资签收系统 - ARM64 Docker 镜像构建脚本
# 适用于 MikroTik hAP ax2 (ARM64/aarch64)
# ============================================================
set -e

IMAGE_NAME="salary-system"
IMAGE_TAG="latest"
TAR_FILE="salary-system-arm64.tar.gz"

echo "============================================"
echo "  构建工资签收系统 ARM64 Docker 镜像"
echo "  目标设备: MikroTik hAP ax2"
echo "============================================"

# 检查 docker buildx 是否可用
if ! docker buildx version &>/dev/null; then
    echo "[ERROR] docker buildx 不可用，请先安装/启用 buildx 插件"
    exit 1
fi

# 确保 arm64 构建器存在
BUILDER_NAME="arm64-builder"
if ! docker buildx inspect "$BUILDER_NAME" &>/dev/null; then
    echo "[INFO] 创建 arm64 buildx 构建器..."
    docker buildx create --name "$BUILDER_NAME" --platform linux/arm64
fi
docker buildx use "$BUILDER_NAME"

echo "[INFO] 开始构建 ARM64 镜像..."
docker buildx build \
    --platform linux/arm64 \
    -t "${IMAGE_NAME}:${IMAGE_TAG}" \
    --load \
    .

echo "[INFO] 镜像构建完成！"
echo ""

# 显示镜像信息
docker images "${IMAGE_NAME}:${IMAGE_TAG}"

echo ""
echo "[INFO] 导出镜像为 tar.gz 文件..."
docker save "${IMAGE_NAME}:${IMAGE_TAG}" | gzip > "$TAR_FILE"

FILE_SIZE=$(du -h "$TAR_FILE" | cut -f1)
echo "============================================"
echo "  构建完成！"
echo "  镜像文件: $TAR_FILE"
echo "  文件大小: $FILE_SIZE"
echo "============================================"
echo ""
echo "部署到 MikroTik hAP ax2 的步骤:"
echo "  1. 将 $TAR_FILE 上传到路由器:"
echo "     scp $TAR_FILE admin@<路由器IP>:/"
echo ""
echo "  2. 在路由器上加载镜像:"
echo "     /container/add file=salary-system-arm64.tar.gz interface=veth1"
echo ""
echo "  3. 查看容器状态:"
echo "     /container/print"
echo ""
echo "  4. 启动容器:"
echo "     /container/start 0"
echo "============================================"
