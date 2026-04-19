#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""添加users表缺失字段并修复数据"""

import sqlite3

DATABASE_PATH = 'E:/22/database/salary_system.db'

def fix():
    conn = sqlite3.connect(DATABASE_PATH)
    cursor = conn.cursor()
    
    print("步骤1: 检查并添加部门字段...")
    try:
        cursor.execute("ALTER TABLE users ADD COLUMN 部门 TEXT DEFAULT '待分配'")
        print("  ✅ 部门字段已添加")
    except Exception as e:
        if "duplicate column" in str(e).lower():
            print("  ⏭️ 部门字段已存在")
        else:
            print(f"  ❌ {e}")
    
    print("\n步骤2: 检查并添加入职日期字段...")
    try:
        cursor.execute("ALTER TABLE users ADD COLUMN 入职日期 DATE")
        print("  ✅ 入职日期字段已添加")
    except Exception as e:
        if "duplicate column" in str(e).lower():
            print("  ⏭️ 入职日期字段已存在")
        else:
            print(f"  ❌ {e}")
    
    print("\n步骤3: 从summary_table同步初始数据到users表...")
    cursor.execute("""
        UPDATE users SET 
            部门 = (SELECT 部门 FROM summary_table WHERE summary_table.姓名 = users.姓名 LIMIT 1),
            入职日期 = (SELECT 入职日期 FROM summary_table WHERE summary_table.姓名 = users.姓名 LIMIT 1)
        WHERE users.role = 'user'
    """)
    updated = cursor.rowcount
    print(f"  ✅ 已更新 {updated} 条用户记录")
    
    conn.commit()
    
    print("\n步骤4: 验证结果...")
    cursor.execute("SELECT 姓名, 手机号, 部门, 入职日期 FROM users WHERE role='user'")
    for row in cursor.fetchall():
        print(f"  {row[0]} - 手机号:{row[1]} 部门:{row[2]} 入职日期:{row[3]}")
    
    conn.close()
    print("\n✅ 完成！")

if __name__ == "__main__":
    fix()