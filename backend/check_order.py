import sqlite3

conn = sqlite3.connect('../database/salary_system.db')
cursor = conn.cursor()

print("=== Summary表按序号排序 ===")
cursor.execute("SELECT 姓名, 序号, 部门 FROM summary_table ORDER BY 序号")
for r in cursor.fetchall():
    print(f"  序号: '{r[1]}', 姓名: {r[0]}, 部门: {r[2]}")

print("\n=== Summary表按CAST(序号 AS INTEGER)排序 ===")
cursor.execute("SELECT 姓名, 序号, 部门 FROM summary_table ORDER BY CAST(序号 AS INTEGER)")
for r in cursor.fetchall():
    print(f"  序号: '{r[1]}', 姓名: {r[0]}, 部门: {r[2]}")

conn.close()
