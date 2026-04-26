#!/bin/bash
# 一键更新容器 - 最简单的方式
# 使用方法: bash quick_update.sh

CONTAINER="hrtools"
HOST_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "正在更新容器 $CONTAINER ..."

# 同步所有文件（排除database和.git）
rsync -avz --delete \
    --exclude='database/' \
    --exclude='.git/' \
    --exclude='app/' \
    --exclude='*.pyc' \
    --exclude='__pycache__/' \
    "$HOST_DIR/" \
    "$(docker inspect -f '{{.Mounts}}' $CONTAINER | grep -oP '(?<=Source:)[^ ]+(?= Destination:/app)')/" 2>/dev/null || \
    docker cp "$HOST_DIR/." "$CONTAINER:/app/"

echo "更新完成! 请重启容器: docker restart $CONTAINER"
