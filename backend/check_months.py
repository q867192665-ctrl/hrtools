import sqlite3

conn = sqlite3.connect('../database/salary_system.db')
cursor = conn.cursor()

print("=== Summary表月份 ===")
cursor.execute("SELECT DISTINCT 月份 FROM summary_table WHERE 月份 IS NOT NULL AND 月份 != ''")
for r in cursor.fetchall():
    print(f"  {r[0]}")

print("\n=== Salary表月份 ===")
cursor.execute("SELECT DISTINCT 月份 FROM salary_table WHERE 月份 IS NOT NULL AND 月份 != ''")
for r in cursor.fetchall():
    print(f"  {r[0]}")

print("\n=== Summary表数据量 ===")
cursor.execute("SELECT COUNT(*) FROM summary_table")
print(f"  总记录数: {cursor.fetchone()[0]}")

print("\n=== 按月份统计 ===")
cursor.execute("SELECT 月份, COUNT(*) as cnt FROM summary_table GROUP BY 月份")
for r in cursor.fetchall():
    print(f"  {r[0]}: {r[1]}条")

conn.close()
