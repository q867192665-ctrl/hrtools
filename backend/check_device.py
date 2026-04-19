#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""检查设备绑定和登录记录"""

import sqlite3
import os

DATABASE_PATH = os.path.join(os.path.dirname(__file__), '..', 'database', 'salary_system.db')

def check():
    try:
        conn = sqlite3.connect(DATABASE_PATH)
        cursor = conn.cursor()
        
        print("=== 设备绑定记录 ===")
        cursor.execute("SELECT * FROM device_bindings")
        bindings = cursor.fetchall()
        if bindings:
            for b in bindings:
                print(f"  {b}")
        else:
            print("  (无记录)")
        
        print("\n=== 登录尝试记录 ===")
        cursor.execute("SELECT id, username, device_id, status, ip_address, created_at FROM login_attempts ORDER BY id DESC LIMIT 10")
        attempts = cursor.fetchall()
        if attempts:
            for a in attempts:
                print(f"  ID={a[0]} 用户={a[1]} 设备ID={a[2][:20] if a[2] else None}... 状态={a[3]} IP={a[4]} 时间={a[5]}")
        else:
            print("  (无记录)")
            
    except Exception as e:
        print(f"错误: {e}")

if __name__ == "__main__":
    check()