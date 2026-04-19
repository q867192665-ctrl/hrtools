import sqlite3

conn = sqlite3.connect('../database/salary_system.db')
cursor = conn.cursor()

print("=== Summary表前3条记录 ===")
cursor.execute("SELECT 姓名, 月份, 部门 FROM summary_table LIMIT 5")
for r in cursor.fetchall():
    print(f"  姓名: {r[0]}, 月份: '{r[1]}', 部门: {r[2]}")

print("\n=== 检查salary_table的月份 ===")
cursor.execute("SELECT 姓名, 月份 FROM salary_table WHERE 月份 IS NOT NULL AND 月份 != '' LIMIT 5")
for r in cursor.fetchall():
    print(f"  姓名: {r[0]}, 月份: {r[1]}")

conn.close()
