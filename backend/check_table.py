#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""检查salary_table表结构"""

import sqlite3
import os

DATABASE_PATH = 'E:/22/database/salary_system.db'

def check():
    try:
        conn = sqlite3.connect(DATABASE_PATH)
        cursor = conn.cursor()
        
        print("=== salary_table 表结构 ===")
        cursor.execute("PRAGMA table_info(salary_table)")
        for col in cursor.fetchall():
            pk = " [PK]" if col[5] == 1 else ""
            unique = " [UNIQUE]" if col[3] != 0 else ""
            print(f"  {col[1]:20s} ({col[2]}){pk}{unique}")
        
        print("\n=== 唯一约束/索引 ===")
        cursor.execute("PRAGMA index_list(salary_table)")
        for idx in cursor.fetchall():
            idx_name = idx[1]
            is_unique = idx[2]
            print(f"  索引: {idx_name} (唯一={is_unique})")
            cursor2 = conn.cursor()
            cursor2.execute(f"PRAGMA index_info({idx_name})")
            for info in cursor2.fetchall():
                print(f"    列: {info[2]}")
        
        print("\n=== 当前数据 ===")
        cursor.execute("SELECT id, 姓名, 月份 FROM salary_table ORDER BY id LIMIT 10")
        for row in cursor.fetchall():
            print(f"  ID={row[0]} 姓名={row[1]} 月份={row[2]}")
            
    except Exception as e:
        print(f"错误: {e}")

if __name__ == "__main__":
    check()