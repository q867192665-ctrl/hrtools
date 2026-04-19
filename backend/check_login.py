#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""检查登录尝试表结构"""

import sqlite3
import os

DATABASE_PATH = os.path.join(os.path.dirname(__file__), '..', 'database', 'salary_system.db')

def check():
    try:
        conn = sqlite3.connect(DATABASE_PATH)
        cursor = conn.cursor()
        
        print("=== login_attempts 表结构 ===")
        cursor.execute("PRAGMA table_info(login_attempts)")
        for col in cursor.fetchall():
            print(f"  {col[1]} ({col[2]})")
        
        print("\n=== 登录尝试记录 ===")
        cursor.execute("SELECT * FROM login_attempts ORDER BY id DESC LIMIT 10")
        attempts = cursor.fetchall()
        if attempts:
            for a in attempts:
                print(f"  {a}")
        else:
            print("  (无记录)")
            
    except Exception as e:
        print(f"错误: {e}")

if __name__ == "__main__":
    check()