#!/bin/sh
# hrtools 更新脚本 - 在 Home Assistant (宿主机) 上运行
# 使用方法: sh update_hrtools.sh

CONTAINER_NAME="hrtools"
GITHUB_REPO="https://github.com/q867192665-ctrl/hrtools.git"

echo "============================================================"
echo "hrtools 容器更新脚本"
echo "============================================================"
echo "容器名称: $CONTAINER_NAME"
echo ""

# 检查 docker 是否可用
if ! command -v docker >/dev/null 2>&1; then
    echo "错误: 未找到 docker 命令"
    exit 1
fi

# 检查容器是否存在
if ! docker ps -a | grep -q "$CONTAINER_NAME"; then
    echo "错误: 容器 $CONTAINER_NAME 不存在"
    echo ""
    echo "可用容器:"
    docker ps -a --format "  {{.Names}}\t{{.Image}}\t{{.Status}}"
    exit 1
fi

echo "✓ 找到容器: $CONTAINER_NAME"

# 获取容器状态
CONTAINER_STATE=$(docker inspect --format='{{.State.Status}}' $CONTAINER_NAME)
echo "✓ 容器状态: $CONTAINER_STATE"

# 步骤1: 检查/初始化 Git 仓库并拉取代码
echo ""
echo "============================================================"
echo "步骤 1: 更新代码"
echo "============================================================"

# 先检查应用路径
APP_PATH=$(docker exec $CONTAINER_NAME sh -c 'ls -d /app /home/app /usr/src/app /config /data 2>/dev/null' | head -1)

if [ -z "$APP_PATH" ]; then
    # 尝试查找 app.py 或 backend 目录
    APP_PATH=$(docker exec $CONTAINER_NAME sh -c '
        for p in /app /home/app /usr/src/app /config /data; do
            if [ -d "$p" ] && ([ -f "$p/backend/app.py" ] || [ -f "$p/app.py" ]); then
                echo "$p"
                break
            fi
        done
        find / -maxdepth 4 -name "app.py" -not -path "*/lib/*" 2>/dev/null | head -1 | xargs dirname 2>/dev/null
    ' | head -1)
fi

if [ -z "$APP_PATH" ]; then
    APP_PATH="/app"
    echo "⚠ 无法自动检测路径，使用默认: $APP_PATH"
else
    echo "✓ 应用路径: $APP_PATH"
fi

# 检查是否有 git
HAS_GIT=$(docker exec $CONTAINER_NAME which git 2>/dev/null)

if [ -n "$HAS_GIT" ]; then
    # 检查是否是 git 仓库
    IS_GIT=$(docker exec $CONTAINER_NAME sh -c "cd $APP_PATH && test -d .git && echo yes || echo no")
    
    if [ "$IS_GIT" = "yes" ]; then
        echo "Git 仓库已存在，拉取最新代码..."
        docker exec $CONTAINER_NAME sh -c "cd $APP_PATH && git pull origin master"
    else
        echo "克隆仓库..."
        docker exec $CONTAINER_NAME sh -c "
            cd $APP_PATH && \
            git clone $GITHUB_REPO .tmp_update && \
            cp -rf .tmp_update/* .tmp_update/.* . 2>/dev/null; \
            rm -rf .tmp_update
        "
    fi
else
    echo "⚠ 容器内没有 git，尝试使用 wget/curl 下载..."

    # 使用 wget 或 curl 下载更新脚本
    HAS_WGET=$(docker exec $CONTAINER_NAME which wget 2>/dev/null)
    HAS_CURL=$(docker exec $CONTAINER_NAME which curl 2>/dev/null)

    if [ -n "$HAS_WGET" ]; then
        echo "使用 wget 下载..."
        docker exec $CONTAINER_NAME sh -c "wget -qO- https://raw.githubusercontent.com/q867192665-ctrl/hrtools/master/update_db_app_versions.py > $APP_PATH/update_db_app_versions.py"
    elif [ -n "$HAS_CURL" ]; then
        echo "使用 curl 下载..."
        docker exec $CONTAINER_NAME sh -c "curl -sL https://raw.githubusercontent.com/q867192665-ctrl/hrtools/master/update_db_app_versions.py > $APP_PATH/update_db_app_versions.py"
    else
        echo "⚠ 容器内没有 wget/curl/git，无法自动更新代码"
        echo "请手动将文件复制到容器中"
    fi
fi

# 步骤2: 更新数据库
echo ""
echo "============================================================"
echo "步骤 2: 更新数据库"
echo "============================================================"

# 查找数据库文件
DB_PATH=$(docker exec $CONTAINER_NAME sh -c "find $APP_PATH -name '*.db' -type f 2>/dev/null | head -1")

if [ -z "$DB_PATH" ]; then
    # 扩大搜索范围
    DB_PATH=$(docker exec $CONTAINER_NAME sh -c "find / -name 'salary_system.db' -type f 2>/dev/null | head -1")
fi

if [ -n "$DB_PATH" ]; then
    echo "✓ 数据库位置: $DB_PATH"
    
    # 检查 sqlite3
    HAS_SQLITE3=$(docker exec $CONTAINER_NAME which sqlite3 2>/dev/null)
    
    if [ -n "$HAS_SQLITE3" ]; then
        echo "使用 sqlite3 创建表..."
        docker exec $CONTAINER_NAME sqlite3 "$DB_PATH" "CREATE TABLE IF NOT EXISTS app_versions (
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
        echo "✓ 表创建完成"
        
        # 显示记录
        echo ""
        echo "版本记录:"
        docker exec $CONTAINER_NAME sh -c "sqlite3 -header -column '$DB_PATH' 'SELECT id, version, is_active, upload_time FROM app_versions ORDER BY upload_time DESC LIMIT 5;' 2>/dev/null || echo '(暂无记录)'"
    else
        # 尝试使用 python
        HAS_PYTHON=$(docker exec $CONTAINER_NAME which python3 2>/dev/null || docker exec $CONTAINER_NAME which python 2>/dev/null)
        
        if [ -n "$HAS_PYTHON" ]; then
            echo "使用 Python 创建表..."
            docker exec $CONTAINER_NAME sh -c "$HAS_PYTHON -c \"
import sqlite3, os
db = '$DB_PATH'
if os.path.exists(db):
    conn = sqlite3.connect(db)
    conn.execute('''CREATE TABLE IF NOT EXISTS app_versions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        version TEXT NOT NULL,
        version_name TEXT,
        file_name TEXT NOT NULL,
        file_path TEXT NOT NULL,
        file_size INTEGER,
        update_note TEXT,
        upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
        is_active INTEGER DEFAULT 1
    )''')
    conn.commit()
    print('Table created successfully!')
else:
    print('Database not found')
\""
            echo "✓ 完成"
        else
            echo "⚠ 容器内没有 sqlite3 和 python"
            echo "请安装其中一个工具或手动执行 SQL"
        fi
    fi
else
    echo "⚠ 未找到数据库文件"
    echo "搜索过的路径: $APP_PATH 及其子目录"
fi

# 步骤3: 重启容器
echo ""
echo "============================================================"
echo "步骤 3: 重启容器"
echo "============================================================"

docker restart $CONTAINER_NAME
echo "✓ 容器已重启"

# 等待启动
echo ""
echo "等待服务启动..."
sleep 5

# 检查状态
NEW_STATE=$(docker inspect --format='{{.State.Status}}' $CONTAINER_NAME)
echo "✓ 当前状态: $NEW_STATE"

echo ""
echo "============================================================"
echo "更新完成!"
echo "============================================================"
echo ""
echo "后续操作:"
echo "1. 访问管理后台: http://yaohu.dynv6.net:32996/admin"
echo "2. 进入 APP 版本管理页面"
echo "3. 上传新版本 APK (版本号: 1.0.4)"
echo "4. 用户登录时将自动检测更新"
echo ""
