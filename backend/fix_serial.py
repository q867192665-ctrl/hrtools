import sqlite3

conn = sqlite3.connect('../database/salary_system.db')
cursor = conn.cursor()

print("=== 修复summary_table中的NEW序号 ===")

cursor.execute("""
    UPDATE summary_table 
    SET 序号 = (
        SELECT s.序号 
        FROM salary_table s 
        WHERE s.姓名 = summary_table.姓名 
        AND s.月份 = summary_table.月份
        LIMIT 1
    )
    WHERE 序号 LIKE 'NEW-%'
""")

affected = cursor.rowcount
conn.commit()
print(f"已修复 {affected} 条记录")

print("\n=== 验证修复结果 ===")
cursor.execute("SELECT 姓名, 序号, 部门 FROM summary_table ORDER BY 序号")
for r in cursor.fetchall():
    print(f"  姓名: {r[0]}, 序号: '{r[1]}', 部门: {r[2]}")

conn.close()
