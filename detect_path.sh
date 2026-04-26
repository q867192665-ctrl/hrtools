#!/bin/sh
# 检测脚本 - 查找应用目录和数据库位置
# 使用方法: sh detect_path.sh

echo "============================================================"
echo "检测应用目录和数据库位置"
echo "============================================================"

echo ""
echo "搜索数据库文件..."
find / -name "salary_system.db" 2>/dev/null

echo ""
echo "搜索 backend 目录..."
find / -type d -name "backend" 2>/dev/null

echo ""
echo "搜索 app.py 文件..."
find / -name "app.py" 2>/dev/null

echo ""
echo "搜索 .git 目录..."
find / -type d -name ".git" 2>/dev/null

echo ""
echo "检查常见路径..."
for dir in /app /home/app /opt/app /usr/src/app /data /config; do
    if [ -d "$dir" ]; then
        echo "存在: $dir"
        ls -la "$dir" 2>/dev/null | head -5
    fi
done

echo ""
echo "============================================================"
echo "检测完成"
echo "============================================================"
