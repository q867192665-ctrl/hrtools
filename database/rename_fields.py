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
    
    print("步骤1: 删除旧视图...")
    try:
        cursor.execute("DROP VIEW IF EXISTS v_summary_full")
        print("  视图已删除")
    except Exception as e:
        print(f"  删除视图失败: {e}")
    
    print("\n步骤2: 重命名字段...")
    
    renames = [
        ('salary_table', '其它捐款', '其它扣款'),
        ('salary_table', '住宿捐款', '住宿扣款'),
        ('salary_table', '水电捐款', '水电扣款'),
        ('summary_table', '其它捐款', '其它扣款'),
        ('summary_table', '住宿捐款', '住宿扣款'),
        ('summary_table', '水电捐款', '水电扣款'),
    ]
    
    for table, old_col, new_col in renames:
        try:
            sql = f'ALTER TABLE {table} RENAME COLUMN "{old_col}" TO "{new_col}"'
            print(f'  执行: {sql}')
            cursor.execute(sql)
            print(f'  成功: {table}.{old_col} -> {new_col}')
        except Exception as e:
            print(f'  失败: {e}')
    
    print("\n步骤3: 验证字段...")
    cursor.execute('PRAGMA table_info(salary_table)')
    print("  salary_table 字段:")
    for col in cursor.fetchall():
        if '扣款' in col[1] or '捐款' in col[1]:
            print(f"    {col[1]}")
    
    cursor.execute('PRAGMA table_info(summary_table)')
    print("  summary_table 字段:")
    for col in cursor.fetchall():
        if '扣款' in col[1] or '捐款' in col[1]:
            print(f"    {col[1]}")
    
    conn.commit()
    conn.close()
    print("\n字段重命名完成！")

if __name__ == '__main__':
    migrate()
