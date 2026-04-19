#!/bin/bash
set -e

echo "============================================"
echo "  人事管理系统 - 容器启动"
echo "  架构: $(uname -m)"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================"

# ---- 数据库初始化 ----
DB_PATH="${DATABASE_PATH:-/app/database/salary_system.db}"
DB_DIR="$(dirname "$DB_PATH")"

if [ ! -f "$DB_PATH" ]; then
    echo "[INFO] 数据库不存在，正在初始化..."
    mkdir -p "$DB_DIR"
    cd /app/database
    python init_database.py
    echo "[INFO] 数据库初始化完成: $DB_PATH"
else
    echo "[INFO] 数据库已存在: $DB_PATH"
fi

echo "[INFO] 检查并修复用户密码..."
cd /app/database
python -c "
import sqlite3
from werkzeug.security import generate_password_hash, check_password_hash
conn = sqlite3.connect('$DB_PATH')
cursor = conn.cursor()

cursor.execute(\"SELECT 姓名, 密码 FROM users WHERE 姓名='admin'\")
row = cursor.fetchone()
if not row:
    hashed = generate_password_hash('admin123')
    cursor.execute(\"INSERT INTO users (姓名, 密码, role) VALUES (?, ?, 'admin')\", ('admin', hashed))
    conn.commit()
    print('[OK] admin用户已创建，密码: admin123')
elif not row[1] or row[1] == '__INITIAL_PASSWORD__' or not check_password_hash(row[1], 'admin123'):
    hashed = generate_password_hash('admin123')
    cursor.execute(\"UPDATE users SET 密码=? WHERE 姓名='admin'\", (hashed,))
    conn.commit()
    print('[OK] admin密码已修复，密码: admin123')
else:
    print('[OK] admin密码正常')

cursor.execute(\"SELECT 姓名, 密码 FROM users WHERE 姓名!='admin' AND role='user'\")
rows = cursor.fetchall()
for r in rows:
    if not r[1] or r[1] == '__INITIAL_PASSWORD__' or not check_password_hash(r[1], '123456'):
        hashed = generate_password_hash('123456')
        cursor.execute(\"UPDATE users SET 密码=? WHERE 姓名=?\", (hashed, r[0]))
        conn.commit()
        print(f'[OK] {r[0]}密码已修复，密码: 123456')

conn.close()
"

echo "[INFO] 检查并迁移数据库结构..."
python -c "
import sqlite3
conn = sqlite3.connect('$DB_PATH')
cursor = conn.cursor()

# 检查并创建缺失的表
required_tables = {
    'salary_table': '''CREATE TABLE salary_table (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        序号 TEXT NOT NULL, 部门 TEXT NOT NULL, 姓名 TEXT NOT NULL UNIQUE, 岗位 TEXT,
        入职日期 DATE NOT NULL, 应出勤天数 DECIMAL(4,1), 实际出勤天数 DECIMAL(4,1),
        上门服务小时 INTEGER, 基本工资底薪 DECIMAL(10,2), 基本工资绩效 DECIMAL(10,2),
        基本工资其它补贴 DECIMAL(10,2), 基本工资合计 DECIMAL(10,2), 岗位工资 DECIMAL(10,2),
        护理员绩效工资 DECIMAL(10,2), 交通费 DECIMAL(10,2), 手机费 DECIMAL(10,2),
        奖金 DECIMAL(10,2), 高温费 DECIMAL(10,2), 应发工资 DECIMAL(10,2),
        应扣款项养老 DECIMAL(10,2), 应扣款项医疗 DECIMAL(10,2), 应扣款项失业 DECIMAL(10,2),
        应扣款项公积金 DECIMAL(10,2), 应扣款项应缴个税 DECIMAL(10,2), 应扣款项缺勤扣款 DECIMAL(10,2),
        其它扣款 DECIMAL(10,2), 住宿扣款 DECIMAL(10,2), 水电扣款 DECIMAL(10,2),
        应扣款项社保 DECIMAL(10,2), 应扣款项个税 DECIMAL(10,2), 应扣款项其他 DECIMAL(10,2),
        应扣款项合计 DECIMAL(10,2), 实发工资 DECIMAL(10,2), 是否代扣社保 BOOLEAN,
        备注 TEXT, 月份 TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )''',
    'summary_table': '''CREATE TABLE summary_table (
        id INTEGER PRIMARY KEY AUTOINCREMENT, 序号 TEXT, 部门 TEXT,
        姓名 TEXT NOT NULL, 岗位 TEXT, 手机号 TEXT, 入职日期 DATE,
        应出勤天数 DECIMAL(4,1), 实际出勤天数 DECIMAL(4,1), 上门服务小时 INTEGER,
        基本工资底薪 DECIMAL(10,2), 基本工资绩效 DECIMAL(10,2), 基本工资合计 DECIMAL(10,2),
        岗位工资 DECIMAL(10,2), 护理员绩效工资 DECIMAL(10,2), 应发工资 DECIMAL(10,2),
        应扣款项社保 DECIMAL(10,2), 应扣款项公积金 DECIMAL(10,2), 应扣款项个税 DECIMAL(10,2),
        应扣款项其他 DECIMAL(10,2), 应扣款项合计 DECIMAL(10,2), 实发工资 DECIMAL(10,2),
        是否代扣社保 BOOLEAN, 查询状态 TEXT DEFAULT '未查询', 查询次数 INTEGER DEFAULT 0,
        签收状态 TEXT DEFAULT '未签收', 最新签收时间 TIMESTAMP, 签收方式 TEXT,
        签名图片 TEXT, 查询数据编号 TEXT, 月份 TEXT,
        基本工资其它补贴 DECIMAL(10,2), 交通费 DECIMAL(10,2), 手机费 DECIMAL(10,2),
        奖金 DECIMAL(10,2), 高温费 DECIMAL(10,2), 应扣款项缺勤扣款 DECIMAL(10,2),
        应扣款项养老 DECIMAL(10,2), 应扣款项医疗 DECIMAL(10,2), 应扣款项失业 DECIMAL(10,2),
        应扣款项应缴个税 DECIMAL(10,2), 其它扣款 DECIMAL(10,2), 住宿扣款 DECIMAL(10,2),
        水电扣款 DECIMAL(10,2),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, version INTEGER DEFAULT 1
    )''',
    'users': '''CREATE TABLE users (
        id INTEGER PRIMARY KEY AUTOINCREMENT, 姓名 TEXT UNIQUE NOT NULL,
        密码 TEXT, 手机号 TEXT, 部门 TEXT, 入职日期 DATE, role TEXT DEFAULT 'user',
        token TEXT, token_expire_time TIMESTAMP, last_login_time TIMESTAMP,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )'''
}

for table_name, create_sql in required_tables.items():
    cursor.execute(f\"SELECT name FROM sqlite_master WHERE type='table' AND name='{table_name}'\")
    if not cursor.fetchone():
        cursor.execute(create_sql)
        print(f'[OK] {table_name}表已创建')

cursor.execute(\"PRAGMA table_info(users)\")
columns = [col[1] for col in cursor.fetchall()]
if '手机号' not in columns:
    cursor.execute(\"ALTER TABLE users ADD COLUMN 手机号 TEXT\")
    print('[OK] users表添加字段: 手机号')
if '部门' not in columns:
    cursor.execute(\"ALTER TABLE users ADD COLUMN 部门 TEXT\")
    print('[OK] users表添加字段: 部门')
if '入职日期' not in columns:
    cursor.execute(\"ALTER TABLE users ADD COLUMN 入职日期 DATE\")
    print('[OK] users表添加字段: 入职日期')

cursor.execute(\"PRAGMA table_info(salary_table)\")
salary_columns = [col[1] for col in cursor.fetchall()]
if '月份' not in salary_columns:
    cursor.execute(\"ALTER TABLE salary_table ADD COLUMN 月份 TEXT\")
    print('[OK] salary_table表添加字段: 月份')
if '基本工资其它补贴' not in salary_columns:
    cursor.execute(\"ALTER TABLE salary_table ADD COLUMN 基本工资其它补贴 DECIMAL(10,2)\")
    print('[OK] salary_table表添加字段: 基本工资其它补贴')
if '交通费' not in salary_columns:
    cursor.execute(\"ALTER TABLE salary_table ADD COLUMN 交通费 DECIMAL(10,2)\")
    print('[OK] salary_table表添加字段: 交通费')
if '手机费' not in salary_columns:
    cursor.execute(\"ALTER TABLE salary_table ADD COLUMN 手机费 DECIMAL(10,2)\")
    print('[OK] salary_table表添加字段: 手机费')
if '奖金' not in salary_columns:
    cursor.execute(\"ALTER TABLE salary_table ADD COLUMN 奖金 DECIMAL(10,2)\")
    print('[OK] salary_table表添加字段: 奖金')
if '高温费' not in salary_columns:
    cursor.execute(\"ALTER TABLE salary_table ADD COLUMN 高温费 DECIMAL(10,2)\")
    print('[OK] salary_table表添加字段: 高温费')
if '应扣款项养老' not in salary_columns:
    cursor.execute(\"ALTER TABLE salary_table ADD COLUMN 应扣款项养老 DECIMAL(10,2)\")
    print('[OK] salary_table表添加字段: 应扣款项养老')
if '应扣款项医疗' not in salary_columns:
    cursor.execute(\"ALTER TABLE salary_table ADD COLUMN 应扣款项医疗 DECIMAL(10,2)\")
    print('[OK] salary_table表添加字段: 应扣款项医疗')
if '应扣款项失业' not in salary_columns:
    cursor.execute(\"ALTER TABLE salary_table ADD COLUMN 应扣款项失业 DECIMAL(10,2)\")
    print('[OK] salary_table表添加字段: 应扣款项失业')
if '应扣款项应缴个税' not in salary_columns:
    cursor.execute(\"ALTER TABLE salary_table ADD COLUMN 应扣款项应缴个税 DECIMAL(10,2)\")
    print('[OK] salary_table表添加字段: 应扣款项应缴个税')
if '应扣款项缺勤扣款' not in salary_columns:
    cursor.execute(\"ALTER TABLE salary_table ADD COLUMN 应扣款项缺勤扣款 DECIMAL(10,2)\")
    print('[OK] salary_table表添加字段: 应扣款项缺勤扣款')
if '其它扣款' not in salary_columns:
    cursor.execute(\"ALTER TABLE salary_table ADD COLUMN 其它扣款 DECIMAL(10,2)\")
    print('[OK] salary_table表添加字段: 其它扣款')
if '住宿扣款' not in salary_columns:
    cursor.execute(\"ALTER TABLE salary_table ADD COLUMN 住宿扣款 DECIMAL(10,2)\")
    print('[OK] salary_table表添加字段: 住宿扣款')
if '水电扣款' not in salary_columns:
    cursor.execute(\"ALTER TABLE salary_table ADD COLUMN 水电扣款 DECIMAL(10,2)\")
    print('[OK] salary_table表添加字段: 水电扣款')

cursor.execute(\"PRAGMA table_info(summary_table)\")
summary_columns = [col[1] for col in cursor.fetchall()]
if '月份' not in summary_columns:
    cursor.execute(\"ALTER TABLE summary_table ADD COLUMN 月份 TEXT\")
    print('[OK] summary_table表添加字段: 月份')
if '手机号' not in summary_columns:
    cursor.execute(\"ALTER TABLE summary_table ADD COLUMN 手机号 TEXT\")
    print('[OK] summary_table表添加字段: 手机号')
if '岗位' not in summary_columns:
    cursor.execute(\"ALTER TABLE summary_table ADD COLUMN 岗位 TEXT\")
    print('[OK] summary_table表添加字段: 岗位')

cursor.execute(\"SELECT name FROM sqlite_master WHERE type='table' AND name='customer_archive'\")
if not cursor.fetchone():
    cursor.execute('''
        CREATE TABLE customer_archive (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            档案编号 TEXT, 联系人 TEXT, 年龄 TEXT, 性别 TEXT, 出生日期 TEXT,
            街道 TEXT, 级别 TEXT, 身份证 TEXT, 联系电话 TEXT, 现住址 TEXT,
            自述身高 TEXT, 自述体重 TEXT, 紧急联系电话 TEXT, 居住情况 TEXT,
            慢性疾病 TEXT, 使用药物 TEXT, 意识 TEXT, 生命体征 TEXT,
            四肢活动情况 TEXT, 现在有无压疮 TEXT, 部位 TEXT,
            压疮危险度评估 TEXT, 日常生活活动能力评估 TEXT,
            跌倒坠床风险评估 TEXT, 生活自理能力 TEXT, 客户类型 TEXT,
            护理计划起始时间 TEXT, 待遇结束时间 TEXT, 护理计划及要点 TEXT,
            备注 TEXT, 上传批次 TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    print('[OK] customer_archive表已创建')

cursor.execute(\"SELECT name FROM sqlite_master WHERE type='table' AND name='device_bindings'\")
if not cursor.fetchone():
    cursor.execute('''
        CREATE TABLE device_bindings (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT NOT NULL, device_id TEXT NOT NULL, device_info TEXT,
            bound_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, last_login_at TIMESTAMP, is_active INTEGER DEFAULT 1
        )
    ''')
    print('[OK] device_bindings表已创建')

cursor.execute(\"SELECT name FROM sqlite_master WHERE type='table' AND name='login_attempts'\")
if not cursor.fetchone():
    cursor.execute('''
        CREATE TABLE login_attempts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT NOT NULL, device_id TEXT, device_info TEXT,
            attempt_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            status TEXT, matched_username TEXT, ip_address TEXT
        )
    ''')
    print('[OK] login_attempts表已创建')

conn.commit()
conn.close()
print('[OK] 数据库迁移完成')
"

echo "[INFO] 同步用户部门信息..."
python -c "
import sqlite3
conn = sqlite3.connect('$DB_PATH')
cursor = conn.cursor()

cursor.execute('''
    UPDATE users SET 部门 = (
        SELECT 部门 FROM salary_table WHERE salary_table.姓名 = users.姓名 LIMIT 1
    ) WHERE 部门 IS NULL AND EXISTS (
        SELECT 1 FROM salary_table WHERE salary_table.姓名 = users.姓名
    )
''')
updated = cursor.rowcount
if updated > 0:
    conn.commit()
    print(f'[OK] 已同步 {updated} 个用户的部门信息')
else:
    print('[OK] 用户部门信息无需同步')

cursor.execute('''
    UPDATE users SET 入职日期 = (
        SELECT 入职日期 FROM salary_table WHERE salary_table.姓名 = users.姓名 LIMIT 1
    ) WHERE 入职日期 IS NULL AND EXISTS (
        SELECT 1 FROM salary_table WHERE salary_table.姓名 = users.姓名
    )
''')
updated = cursor.rowcount
if updated > 0:
    conn.commit()
    print(f'[OK] 已同步 {updated} 个用户的入职日期')

conn.close()
"

# ---- 签名目录 ----
SIG_DIR="${SIGNATURE_DIR:-/app/backend/signatures}"
mkdir -p "$SIG_DIR"
mkdir -p /app/backend/exports

# ---- 启动应用 ----
echo "[INFO] 启动人事管理系统..."
echo "[INFO] 监听地址: [::]:32996 (IPv4+IPv6)"
echo "[INFO] Web管理界面: http://localhost:32996/login"
echo "============================================"

cd /app/backend

# 执行传入的 CMD（默认为 gunicorn）
exec "$@"
