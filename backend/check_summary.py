#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""检查summary_table数据和月份"""

import sqlite3

DATABASE_PATH = 'E:/22/database/salary_system.db'

def check():
    conn = sqlite3.connect(DATABASE_PATH)
    cursor = conn.cursor()
    
    print("=== summary_table 数据 ===")
    cursor.execute("SELECT id, 姓名, 月份, 部门, 手机号, 入职日期 FROM summary_table ORDER BY id")
    rows = cursor.fetchall()
    
    if rows:
        for row in rows:
            print(f"  ID={row[0]} 姓名={row[1]} 月份='{row[2]}' 部门={row[3]} 手机号={row[4]} 入职日期={row[5]}")
        
        print(f"\n  总记录数: {len(rows)}")
        print(f"\n=== 月份分布 ===")
        cursor.execute("SELECT 月份, COUNT(*) as cnt FROM summary_table GROUP BY 月份")
        for row in cursor.fetchall():
            print(f"  月份='{row[0]}' 数量={row[1]}")
    else:
        print("  (无数据)")
    
    print("\n=== users表数据(手机号/部门/入职日期) ===")
    cursor.execute("SELECT 姓名, 手机号, 部门, 入职日期 FROM users WHERE role='user'")
    for row in cursor.fetchall():
        print(f"  {row[0]} - 手机号:{row[1]} 部门:{row[2]} 入职日期:{row[3]}")

if __name__ == "__main__":
    check()