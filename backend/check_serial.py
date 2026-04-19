import sqlite3

conn = sqlite3.connect('../database/salary_system.db')
cursor = conn.cursor()

print("=== 检查summary_table序号字段 ===")
cursor.execute("SELECT 姓名, 序号, 部门 FROM summary_table ORDER BY 序号")
for r in cursor.fetchall():
    print(f"  姓名: {r[0]}, 序号: '{r[1]}', 部门: {r[2]}")

print("\n=== 检查salary_table序号字段（前10条）===")
cursor.execute("SELECT 姓名, 序号 FROM salary_table LIMIT 10")
for r in cursor.fetchall():
    print(f"  姓名: {r[0]}, 序号: '{r[1]}'")

conn.close()
