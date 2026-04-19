#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
数据库迁移脚本 - 重命名字段：捐款改为扣款
"""

import sqlite3

DATABASE_PATH = 'E:/22/database/salary_system.db'

def migrate():
    conn = sqlite3.connect(DATABASE_PATH)
    cursor = conn.cursor()
    
    print("开始重命名字段...")
    
    rename_mappings = [
        ('salary_table', '其它捐款', '其它扣款'),
        ('salary_table', '住宿捐款', '住宿扣款'),
        ('salary_table', '水电捐款', '水电扣款'),
        ('summary_table', '其它捐款', '其它扣款'),
        ('summary_table', '住宿捐款', '住宿扣款'),
        ('summary_table', '水电捐款', '水电扣款'),
    ]
    
    for table, old_name, new_name in rename_mappings:
        try:
            cursor.execute(f"ALTER TABLE {table} RENAME COLUMN {old_name} TO {new_name}")
            print(f"  {table}.{old_name} -> {new_name} 成功")
        except Exception as e:
            if "no such column" in str(e).lower():
                print(f"  {table}.{old_name} 不存在，跳过")
            elif "duplicate column name" in str(e).lower():
                print(f"  {table}.{new_name} 已存在")
            else:
                print(f"  {table}.{old_name} -> {new_name} 失败: {e}")
    
    conn.commit()
    conn.close()
    print("字段重命名完成！")

if __name__ == '__main__':
    migrate()
