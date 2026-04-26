#!/bin/sh
# 查找后端服务容器
echo "============================================================"
echo "查找后端服务容器"
echo "============================================================"

echo ""
echo "1. 查看所有运行中的容器..."
docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "2. 搜索包含 'salary' 或 'backend' 的容器..."
docker ps -a | grep -iE "(salary|backend|hrtools|app)" || echo "未找到匹配的容器"

echo ""
echo "3. 查看所有容器的挂载卷..."
docker ps -q | while read cid; do
    name=$(docker inspect --format='{{.Name}}' $cid 2>/dev/null)
    mounts=$(docker inspect --format='{{range .Mounts}}{{.Source}} -> {{.Destination}}{{"\n"}}{{end}}' $cid 2>/dev/null | grep -E "(db|database)" || true)
    if [ -n "$mounts" ]; then
        echo "容器: $name"
        echo "$mounts"
        echo "---"
    fi
done

echo ""
echo "4. 搜索数据库文件位置..."
find /var/lib/docker/volumes/ -name "*.db" 2>/dev/null | head -10

echo ""
echo "============================================================"
echo "请找到你的后端服务容器名称或ID"
echo "============================================================"
