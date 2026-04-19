#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""检查数据库结构"""

import sqlite3
import os

DATABASE_PATH = os.path.join(os.path.dirname(__file__), '..', 'database', 'salary_system.db')

def check_database_structure():
    """检查数据库结构"""
    try:
        conn = sqlite3.connect(DATABASE_PATH)
        cursor = conn.cursor()
        
        # 检查所有表
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = cursor.fetchall()
        print("📊 数据库中的表:")
        for table in tables:
            print(f"   {table[0]}")
        
        # 检查users表结构
        print("\n🔍 users表结构:")
        cursor.execute("PRAGMA table_info(users)")
        columns = cursor.fetchall()
        for col in columns:
            print(f"   {col[1]} ({col[2]}) - {'NOT NULL' if col[3] else 'NULLABLE'}")
        
        # 检查users表中的数据
        print("\n👥 users表中的用户:")
        cursor.execute("SELECT * FROM users")
        users = cursor.fetchall()
        for user in users:
            print(f"   {user}")
            
    except Exception as e:
        print(f"❌ 检查失败: {e}")
    finally:
        if conn:
            conn.close()

if __name__ == "__main__":
    check_database_structure()