#!/bin/bash
# 从宿主机直接更新容器内的文件
# 使用方法: bash update_container_from_host.sh

CONTAINER_NAME="hrtools"
HOST_DIR="$(cd "$(dirname "$0")" && pwd)"
CONTAINER_DIR="/app"

echo "============================================================"
echo "从宿主机更新容器文件"
echo "============================================================"
echo "容器名称: $CONTAINER_NAME"
echo "宿主机目录: $HOST_DIR"
echo "容器目录: $CONTAINER_DIR"
echo ""

# 检查容器是否运行
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "错误: 容器 $CONTAINER_NAME 未运行"
    exit 1
fi

# 获取需要更新的文件列表
echo "步骤 1: 检查需要更新的文件..."
echo ""

UPDATED_FILES=()
SKIPPED_FILES=0

# 遍历宿主机文件（排除database目录和.git目录）
while IFS= read -r -d '' file; do
    REL_PATH="${file#$HOST_DIR/}"
    
    # 跳过特定目录
    if [[ "$REL_PATH" == database/* ]] || [[ "$REL_PATH" == .git/* ]] || [[ "$REL_PATH" == app/* ]]; then
        continue
    fi
    
    CONTAINER_FILE="$CONTAINER_DIR/$REL_PATH"
    
    # 检查文件是否存在于容器中
    if docker exec "$CONTAINER_NAME" test -f "$CONTAINER_FILE" 2>/dev/null; then
        # 比较文件内容
        HOST_HASH=$(sha256sum "$file" | awk '{print $1}')
        CONTAINER_HASH=$(docker exec "$CONTAINER_NAME" sha256sum "$CONTAINER_FILE" 2>/dev/null | awk '{print $1}')
        
        if [ "$HOST_HASH" != "$CONTAINER_HASH" ]; then
            echo "  [更新] $REL_PATH"
            UPDATED_FILES+=("$REL_PATH")
        else
            SKIPPED_FILES=$((SKIPPED_FILES + 1))
        fi
    else
        echo "  [新增] $REL_PATH"
        UPDATED_FILES+=("$REL_PATH")
    fi
done < <(find "$HOST_DIR" -type f -print0)

echo ""
echo "需要更新: ${#UPDATED_FILES[@]} 个文件, 跳过: $SKIPPED_FILES 个文件"
echo ""

if [ ${#UPDATED_FILES[@]} -eq 0 ]; then
    echo "所有文件都是最新的，无需更新"
    exit 0
fi

# 复制文件到容器
echo "步骤 2: 复制文件到容器..."
echo ""

SUCCESS_COUNT=0
FAIL_COUNT=0

for REL_PATH in "${UPDATED_FILES[@]}"; do
    HOST_FILE="$HOST_DIR/$REL_PATH"
    CONTAINER_FILE="$CONTAINER_DIR/$REL_PATH"
    
    # 确保目录存在
    CONTAINER_DIR_PATH=$(dirname "$CONTAINER_FILE")
    docker exec "$CONTAINER_NAME" mkdir -p "$CONTAINER_DIR_PATH" 2>/dev/null
    
    # 复制文件
    if docker cp "$HOST_FILE" "$CONTAINER_NAME:$CONTAINER_FILE" 2>/dev/null; then
        echo "  [成功] $REL_PATH"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        echo "  [失败] $REL_PATH"
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
done

echo ""
echo "复制完成: 成功 $SUCCESS_COUNT 个, 失败 $FAIL_COUNT 个"
echo ""

# 重启服务
echo "步骤 3: 重启容器服务..."
docker exec "$CONTAINER_NAME" sh -c "supervisorctl restart all 2>/dev/null || systemctl restart gunicorn 2>/dev/null || echo '提示: 请手动重启容器'"

echo ""
echo "============================================================"
echo "更新完成!"
echo "============================================================"
