#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
数据库迁移：添加重签原因字段
"""

import sqlite3
import os

DATABASE_PATH = os.path.join(os.path.dirname(__file__), 'salary_system.db')

def migrate():
    """执行数据库迁移"""
    conn = sqlite3.connect(DATABASE_PATH)
    cursor = conn.cursor()
    
    try:
        # 检查字段是否已存在
        cursor.execute("PRAGMA table_info(summary_table)")
        columns = [column[1] for column in cursor.fetchall()]
        
        if '重签原因' not in columns:
            print("[INFO] 添加 '重签原因' 字段到 summary_table...")
            cursor.execute("ALTER TABLE summary_table ADD COLUMN 重签原因 TEXT")
            conn.commit()
            print("[SUCCESS] 字段添加成功")
        else:
            print("[INFO] '重签原因' 字段已存在，跳过迁移")
        
        # 添加签收时间字段
        if '签收时间' not in columns:
            print("[INFO] 添加 '签收时间' 字段到 summary_table...")
            cursor.execute("ALTER TABLE summary_table ADD COLUMN 签收时间 DATETIME")
            conn.commit()
            print("[SUCCESS] 字段添加成功")
        else:
            print("[INFO] '签收时间' 字段已存在，跳过迁移")
        
    except Exception as e:
        print(f"[ERROR] 迁移失败: {e}")
        conn.rollback()
    finally:
        conn.close()

if __name__ == '__main__':
    print("=" * 60)
    print("数据库迁移：添加重签相关字段")
    print("=" * 60)
    migrate()
    print("=" * 60)
