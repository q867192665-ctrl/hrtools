#!/bin/bash
# 容器内更新脚本 - 从GitHub拉取最新代码并更新程序
# 使用方法: bash update.sh

set -e

echo "============================================================"
echo "容器内更新脚本"
echo "============================================================"

APP_DIR="/app"
DATABASE_PATH="/app/database/salary_system.db"
GITHUB_REPO="https://github.com/q867192665-ctrl/hrtools.git"

cd "$APP_DIR"

# 步骤1: 更新代码
echo ""
echo "============================================================"
echo "步骤 1: 更新代码"
echo "============================================================"

if [ -d ".git" ]; then
    echo "检测到Git仓库，执行 git pull..."
    git pull origin master
else
    echo "未检测到Git仓库，尝试克隆..."
    git clone "$GITHUB_REPO" .
fi

# 步骤2: 更新数据库
echo ""
echo "============================================================"
echo "步骤 2: 更新数据库"
echo "============================================================"

if [ -f "$DATABASE_PATH" ]; then
    echo "运行数据库更新脚本..."
    python3 update_db_app_versions.py
else
    echo "警告: 数据库文件不存在: $DATABASE_PATH"
fi

# 步骤3: 安装依赖
echo ""
echo "============================================================"
echo "步骤 3: 安装依赖"
echo "============================================================"

if [ -f "backend/requirements.txt" ]; then
    echo "安装Python依赖..."
    pip install -r backend/requirements.txt
else
    echo "未找到 requirements.txt，跳过"
fi

# 步骤4: 重启服务
echo ""
echo "============================================================"
echo "步骤 4: 重启服务"
echo "============================================================"

if command -v supervisorctl &> /dev/null; then
    echo "使用supervisorctl重启服务..."
    supervisorctl restart all
elif command -v systemctl &> /dev/null; then
    echo "使用systemctl重启服务..."
    systemctl restart gunicorn 2>/dev/null || echo "gunicorn服务未找到"
else
    echo "提示: 请手动重启服务"
fi

echo ""
echo "============================================================"
echo "更新完成!"
echo "============================================================"
