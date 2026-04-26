#!/bin/sh
# 调试脚本 - 检查版本检查API返回的数据
# 使用方法: sh debug_version_check.sh

echo "============================================================"
echo "调试版本检查功能"
echo "============================================================"

CONTAINER_NAME="hrtools"

echo ""
echo "1. 检查数据库中的版本记录..."
docker exec $CONTAINER_NAME python3 -c "
import sqlite3, json

conn = sqlite3.connect('/app/database/salary_system.db')
cursor = conn.cursor()

print('app_versions 表内容:')
print('-' * 60)
cursor.execute('SELECT id, version, version_name, is_active, upload_time FROM app_versions ORDER BY id DESC LIMIT 5')
rows = cursor.fetchall()
if rows:
    for r in rows:
        print(f'ID: {r[0]}, 版本: {r[1]}, 激活: {r[3]}, 时间: {r[4]}')
else:
    print('(空)')

conn.close()
"

echo ""
echo "2. 测试版本检查 API (模拟APP请求)..."
RESULT=$(curl -s "http://yaohu.dynv6.net:32996/api/app-version/check?version=1.0.3")
echo "API返回结果:"
echo "$RESULT" | python3 -m json.tool 2>/dev/null || echo "$RESULT"

echo ""
echo "3. 解析返回数据..."
echo "$RESULT" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f'success: {data.get(\"success\")}')
    print(f'need_update: {data.get(\"need_update\")}')
    print(f'latest_version: {data.get(\"latest_version\")}')
    print(f'current_version: {data.get(\"current_version\")}')
    
    if data.get('need_update'):
        print('')
        print('✓ 应该提示更新!')
    else:
        print('')
        print('✗ 不会提示更新')
        print('')
        print('可能原因:')
        print('  1. 数据库中没有上传新版本的APK')
        print('  2. 上传的版本号 <= 1.0.3')
        print('  3. 版本解析失败')
except Exception as e:
    print(f'解析错误: {e}')
"

echo ""
echo "============================================================"
