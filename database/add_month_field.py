import sqlite3

DATABASE_PATH = 'E:/22/database/salary_system.db'

conn = sqlite3.connect(DATABASE_PATH)
cursor = conn.cursor()

try:
    cursor.execute('DROP VIEW IF EXISTS v_summary_full')
    print('Dropped view v_summary_full')
except Exception as e:
    print(f'Drop view error: {e}')

try:
    cursor.execute("ALTER TABLE salary_table ADD COLUMN 月份 TEXT DEFAULT ''")
    print('Added 月份 to salary_table')
except Exception as e:
    print(f'salary_table: {e}')

try:
    cursor.execute("ALTER TABLE summary_table ADD COLUMN 月份 TEXT DEFAULT ''")
    print('Added 月份 to summary_table')
except Exception as e:
    print(f'summary_table: {e}')

conn.commit()

cursor.execute("PRAGMA table_info(salary_table)")
cols = cursor.fetchall()
print(f'\nsalary_table columns: {len(cols)}')
for c in cols:
    print(f'  {c[1]} ({c[2]})')

cursor.execute("PRAGMA table_info(summary_table)")
cols = cursor.fetchall()
print(f'\nsummary_table columns: {len(cols)}')
for c in cols:
    print(f'  {c[1]} ({c[2]})')

conn.close()
print('\nDone')
