import sqlite3

conn = sqlite3.connect('../database/salary_system.db')
cursor = conn.cursor()

print("开始修复summary_table的月份字段...")

cursor.execute("""
    UPDATE summary_table 
    SET 月份 = (
        SELECT s.月份 
        FROM salary_table s 
        WHERE s.姓名 = summary_table.姓名 
        AND s.月份 IS NOT NULL AND s.月份 != ''
        LIMIT 1
    )
    WHERE 月份 IS NULL OR 月份 = ''
""")

affected = cursor.rowcount
conn.commit()
print(f"已修复 {affected} 条记录")

print("\n=== 修复后验证 ===")
cursor.execute("SELECT 姓名, 月份, 部门 FROM summary_table LIMIT 5")
for r in cursor.fetchall():
    print(f"  姓名: {r[0]}, 月份: '{r[1]}', 部门: {r[2]}")

cursor.execute("SELECT DISTINCT 月份 FROM summary_table WHERE 月份 IS NOT NULL AND 月份 != ''")
months = [r[0] for r in cursor.fetchall()]
print(f"\n可用月份: {months}")

conn.close()
