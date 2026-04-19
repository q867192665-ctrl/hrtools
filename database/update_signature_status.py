#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sqlite3

conn = sqlite3.connect('e:/22/database/salary_system.db')
cursor = conn.cursor()

# 更新现有签名文件的签收状态
cursor.execute("""
    UPDATE signature_files 
    SET 签收状态='已签收', 签收时间=上传时间 
    WHERE 签收状态 IS NULL OR 签收状态='未签收'
""")
conn.commit()
print(f'已更新 {cursor.rowcount} 条签名记录')

# 查看更新后的结果
cursor.execute("SELECT 文件ID, 用户姓名, 签收状态, 签收时间 FROM signature_files")
rows = cursor.fetchall()
print('\n签名文件表:')
for row in rows:
    print(f'  {row[0]}: 用户={row[1]}, 签收状态={row[2]}, 签收时间={row[3]}')

conn.close()
