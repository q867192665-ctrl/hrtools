#!/bin/sh
# 容器内更新脚本 - 从GitHub拉取最新代码并更新程序
# 使用方法: sh update_alpine.sh
# 适用于 Alpine Linux 容器

echo "============================================================"
echo "容器内更新脚本 (Alpine Linux)"
echo "============================================================"

APP_DIR="/app"
DATABASE_PATH="/app/database/salary_system.db"
GITHUB_REPO="https://github.com/q867192665-ctrl/hrtools.git"

cd "$APP_DIR" 2>/dev/null || {
    echo "错误: 无法进入目录 $APP_DIR"
    exit 1
}

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
    echo "检查 app_versions 表..."
    
    # 使用 sqlite3 命令行工具
    TABLE_EXISTS=$(sqlite3 "$DATABASE_PATH" "SELECT name FROM sqlite_master WHERE type='table' AND name='app_versions';")
    
    if [ -n "$TABLE_EXISTS" ]; then
        echo "app_versions 表已存在"
    else
        echo "创建 app_versions 表..."
        sqlite3 "$DATABASE_PATH" "CREATE TABLE app_versions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            version TEXT NOT NULL,
            version_name TEXT,
            file_name TEXT NOT NULL,
            file_path TEXT NOT NULL,
            file_size INTEGER,
            update_note TEXT,
            upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
            is_active INTEGER DEFAULT 1
        );"
        echo "app_versions 表创建成功!"
    fi
    
    # 显示当前记录
    echo ""
    echo "当前版本记录:"
    sqlite3 -header -column "$DATABASE_PATH" "SELECT id, version, is_active, upload_time FROM app_versions ORDER BY upload_time DESC LIMIT 5;" 2>/dev/null || echo "暂无记录"
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
    # 尝试 pip 或 pip3
    if command -v pip >/dev/null 2>&1; then
        pip install -r backend/requirements.txt
    elif command -v pip3 >/dev/null 2>&1; then
        pip3 install -r backend/requirements.txt
    else
        echo "未找到 pip，跳过依赖安装"
    fi
else
    echo "未找到 requirements.txt，跳过"
fi

# 步骤4: 重启服务
echo ""
echo "============================================================"
echo "步骤 4: 重启服务"
echo "============================================================"

if command -v supervisorctl >/dev/null 2>&1; then
    echo "使用supervisorctl重启服务..."
    supervisorctl restart all
elif command -v s6-svc >/dev/null 2>&1; then
    echo "使用s6重启服务..."
    s6-svc -r /var/run/s6/services/*
else
    echo "提示: 请手动重启服务，或服务可能已自动重载"
fi

echo ""
echo "============================================================"
echo "更新完成!"
echo "============================================================"
echo ""
echo "后续操作:"
echo "1. 在管理后台上传新版 APK (版本号: 1.0.4)"
echo "2. 用户端登录时会自动检测更新"
echo ""
