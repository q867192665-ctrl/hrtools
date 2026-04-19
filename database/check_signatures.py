import sqlite3
import os

db_path = r'e:\22\database\salary_system.db'
conn = sqlite3.connect(db_path)
conn.row_factory = sqlite3.Row
cursor = conn.cursor()

print("=== 签名数据检查 ===\n")

cursor.execute("SELECT 姓名, 签收状态, 签名图片 FROM summary_table")
rows = cursor.fetchall()

for row in rows:
    name = row["姓名"]
    status = row["签收状态"]
    sig_path = row["签名图片"]
    
    print(f"用户: {name}")
    print(f"  签收状态: {status}")
    print(f"  签名路径: {sig_path}")
    
    if sig_path:
        exists = os.path.exists(sig_path)
        print(f"  文件存在: {exists}")
    print()

conn.close()
