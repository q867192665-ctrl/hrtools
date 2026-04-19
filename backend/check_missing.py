#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sqlite3

conn = sqlite3.connect('e:/22/database/salary_system.db')
cursor = conn.cursor()

cursor.execute('SELECT * FROM salary_table')
rows = cursor.fetchall()

cursor.execute('PRAGMA table_info(salary_table)')
cols = [r[1] for r in cursor.fetchall()]

print('工资数据条数:', len(rows))
print()

for row in rows:
    data = dict(zip(cols, row))
    print(f"姓名: {data['姓名']}")
    missing = []
    for col in cols:
        if col not in ['id', 'created_at', 'updated_at', '备注']:
            if data[col] is None or data[col] == '':
                missing.append(col)
    if missing:
        print(f'  缺少字段: {missing}')
    else:
        print('  数据完整')
    print()

conn.close()
