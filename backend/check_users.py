#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""检查users表结构"""

import sqlite3

DATABASE_PATH = 'E:/22/database/salary_system.db'

def check():
    conn = sqlite3.connect(DATABASE_PATH)
    cursor = conn.cursor()
    
    print("=== users 表结构 ===")
    cursor.execute("PRAGMA table_info(users)")
    for col in cursor.fetchall():
        print(f"  {col[1]:20s} ({col[2]})")
    
    print("\n=== users 数据 ===")
    cursor.execute("SELECT * FROM users")
    for row in cursor.fetchall():
        print(f"  {row}")

if __name__ == "__main__":
    check()