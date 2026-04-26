#!/bin/sh
# 宿主机更新脚本 - 更新 hrtools 容器内的应用
# 使用方法: sh update_hrtools.sh
# 在宿主机上运行（不是容器内）

CONTAINER_NAME="hrtools"
GITHUB_REPO="https://github.com/q867192665-ctrl/hrtools.git"

echo "============================================================"
echo "hrtools 容器更新脚本"
echo "============================================================"
echo "容器名称: $CONTAINER_NAME"
echo ""

# 检查容器是否运行
if ! docker ps | grep -q "$CONTAINER_NAME"; then
    echo "错误: 容器 $CONTAINER_NAME 未运行"
    echo ""
    echo "请先启动容器:"
    echo "  docker start $CONTAINER_NAME"
    exit 1
fi

echo "✓ 容器 $CONTAINER_NAME 运行中"

# 获取容器内的应用路径
APP_PATH=$(docker exec $CONTAINER_NAME sh -c 'ls -d /app /home/app /usr/src/app 2>/dev/null' | head -1)
if [ -z "$APP_PATH" ]; then
    # 尝试查找 app.py 所在目录
    APP_PATH=$(docker exec $CONTAINER_NAME sh -c 'find / -maxdepth 3 -name "app.py" -not -path "*/lib/*" 2>/dev/null' | head -1)
    if [ -n "$APP_PATH" ]; then
        APP_PATH=$(dirname "$APP_PATH")
    fi
fi

if [ -z "$APP_PATH" ]; then
    APP_PATH="/app"
    echo "⚠ 无法自动检测应用路径，使用默认路径: $APP_PATH"
else
    echo "✓ 应用路径: $APP_PATH"
fi

# 步骤1: 检查并初始化Git仓库
echo ""
echo "============================================================"
echo "步骤 1: 检查/初始化 Git 仓库"
echo "============================================================"

IS_GIT_REPO=$(docker exec $CONTAINER_NAME sh -c "cd $APP_PATH && test -d .git && echo yes || echo no")

if [ "$IS_GIT_REPO" = "yes" ]; then
    echo "✓ Git 仓库已存在"
else
    echo "正在克隆仓库..."
    docker exec $CONTAINER_NAME sh -c "cd $APP_PATH && git clone $GITHUB_REPO temp_repo && shopt -s dotglob && mv temp_repo/* temp_repo/.* . 2>/dev/null; rm -rf temp_repo"
    echo "✓ 克隆完成"
fi

# 步骤2: 拉取最新代码
echo ""
echo "============================================================"
echo "步骤 2: 拉取最新代码"
echo "============================================================"

docker exec $CONTAINER_NAME sh -c "cd $APP_PATH && git pull origin master"
echo "✓ 代码已更新"

# 步骤3: 更新数据库
echo ""
echo "============================================================"
echo "步骤 3: 更新数据库"
echo "============================================================"

# 查找数据库文件
DB_PATH=$(docker exec $CONTAINER_NAME sh -c "find $APP_PATH -name '*.db' -type f 2>/dev/null" | head -1)

if [ -n "$DB_PATH" ]; then
    echo "✓ 找到数据库: $DB_PATH"
    
    # 检查 sqlite3 是否可用
    HAS_SQLITE3=$(docker exec $CONTAINER_NAME which sqlite3 2>/dev/null)
    
    if [ -n "$HAS_SQLITE3" ]; then
        # 创建 app_versions 表
        docker exec $CONTAINER_NAME sh -c "sqlite3 '$DB_PATH' \"CREATE TABLE IF NOT EXISTS app_versions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            version TEXT NOT NULL,
            version_name TEXT,
            file_name TEXT NOT NULL,
            file_path TEXT NOT NULL,
            file_size INTEGER,
            update_note TEXT,
            upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
            is_active INTEGER DEFAULT 1
        );\""
        
        echo "✓ 数据库表检查完成"
        
        # 显示当前版本记录
        echo ""
        echo "当前版本记录:"
        docker exec $CONTAINER_NAME sh -c "sqlite3 -header -column '$DB_PATH' 'SELECT id, version, is_active, upload_time FROM app_versions ORDER BY upload_time DESC LIMIT 5;' 2>/dev/null || echo '(暂无记录)'"
    else
        echo "⚠ 容器内没有 sqlite3 命令"
        echo "提示: 可以使用 Python 来创建表:"
        echo "  docker exec $CONTAINER_NAME python3 -c \"
import sqlite3; conn = sqlite3.connect('$DB_PATH'); conn.execute('''CREATE TABLE IF NOT EXISTS app_versions (id INTEGER PRIMARY KEY AUTOINCREMENT, version TEXT NOT NULL, version_name TEXT, file_name TEXT NOT NULL, file_path TEXT NOT NULL, file_size INTEGER, update_note TEXT, upload_time DATETIME DEFAULT CURRENT_TIMESTAMP, is_active INTEGER DEFAULT 1)'''); conn.commit(); print('Table created!')\""
    fi
else
    echo "⚠ 未找到数据库文件"
    echo "搜索位置: $APP_PATH 及其子目录"
fi

# 步骤4: 重启容器服务
echo ""
echo "============================================================"
echo "步骤 4: 重启容器"
echo "============================================================"

docker restart $CONTAINER_NAME
echo "✓ 容器已重启"

echo ""
echo "============================================================"
echo "更新完成!"
echo "============================================================"
echo ""
echo "后续操作:"
echo "1. 等待容器完全启动 (约10-20秒)"
echo "2. 访问管理后台: http://yaohu.dynv6.net:32996/admin"
echo "3. 进入 APP 更新管理页面"
echo "4. 上传新版本 APK (版本号: 1.0.4)"
echo "5. 用户登录时会自动检测更新"
echo ""
