#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sqlite3

conn = sqlite3.connect('e:/22/database/salary_system.db')
cursor = conn.cursor()

# 添加签收状态字段
try:
    cursor.execute("ALTER TABLE signature_files ADD COLUMN 签收状态 TEXT DEFAULT '未签收'")
    print('已添加签收状态字段')
except Exception as e:
    print(f'签收状态字段: {e}')

# 添加重签原因字段
try:
    cursor.execute('ALTER TABLE signature_files ADD COLUMN 重签原因 TEXT')
    print('已添加重签原因字段')
except Exception as e:
    print(f'重签原因字段: {e}')

# 添加签收时间字段
try:
    cursor.execute('ALTER TABLE signature_files ADD COLUMN 签收时间 TIMESTAMP')
    print('已添加签收时间字段')
except Exception as e:
    print(f'签收时间字段: {e}')

conn.commit()
conn.close()
print('数据库更新完成')
