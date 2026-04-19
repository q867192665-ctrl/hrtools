import sqlite3
from datetime import datetime

DATABASE_PATH = 'E:/22/database/salary_system.db'

def migrate():
    conn = sqlite3.connect(DATABASE_PATH)
    cursor = conn.cursor()
    
    print("创建设备绑定表...")
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS device_bindings (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT NOT NULL,
            device_id TEXT NOT NULL,
            device_info TEXT,
            bound_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            last_login_at TIMESTAMP,
            is_active INTEGER DEFAULT 1,
            UNIQUE(username, device_id)
        )
    ''')
    print("  设备绑定表创建成功")
    
    print("\n创建异常登录记录表...")
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS login_attempts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT NOT NULL,
            device_id TEXT NOT NULL,
            device_info TEXT,
            attempt_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            status TEXT DEFAULT 'failed',
            matched_username TEXT,
            ip_address TEXT
        )
    ''')
    print("  异常登录记录表创建成功")
    
    print("\n创建索引...")
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_device_bindings_username ON device_bindings(username)')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_device_bindings_device_id ON device_bindings(device_id)')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_login_attempts_username ON login_attempts(username)')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_login_attempts_device_id ON login_attempts(device_id)')
    print("  索引创建成功")
    
    conn.commit()
    conn.close()
    
    print("\n=== 迁移完成 ===")
    
    verify()

def verify():
    conn = sqlite3.connect(DATABASE_PATH)
    cursor = conn.cursor()
    
    print("\n验证表结构:")
    
    cursor.execute("PRAGMA table_info(device_bindings)")
    print("\ndevice_bindings 表:")
    for c in cursor.fetchall():
        print(f'  {c[1]} ({c[2]})')
    
    cursor.execute("PRAGMA table_info(login_attempts)")
    print("\nlogin_attempts 表:")
    for c in cursor.fetchall():
        print(f'  {c[1]} ({c[2]})')
    
    conn.close()

if __name__ == '__main__':
    migrate()
