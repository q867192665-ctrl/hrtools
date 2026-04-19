#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
数据库迁移脚本 - 添加新字段
"""

import sqlite3

DATABASE_PATH = 'e:/22/database/salary_system.db'

def migrate():
    conn = sqlite3.connect(DATABASE_PATH)
    cursor = conn.cursor()
    
    print("开始添加新字段...")
    
    # 添加新字段到 salary_table
    new_salary_fields = [
        ('岗位工资', 'DECIMAL(10,2)'),
        ('交通费', 'DECIMAL(10,2)'),
        ('手机费', 'DECIMAL(10,2)'),
        ('奖金', 'DECIMAL(10,2)'),
        ('高温费', 'DECIMAL(10,2)'),
        ('其它捐款', 'DECIMAL(10,2)'),
        ('住宿捐款', 'DECIMAL(10,2)'),
        ('水电捐款', 'DECIMAL(10,2)')
    ]
    
    for field_name, field_type in new_salary_fields:
        try:
            cursor.execute(f"ALTER TABLE salary_table ADD COLUMN {field_name} {field_type}")
            print(f"  salary_table.{field_name} 添加成功")
        except Exception as e:
            if "duplicate column name" in str(e).lower():
                print(f"  salary_table.{field_name} 已存在")
            else:
                print(f"  salary_table.{field_name} 添加失败: {e}")
    
    # 添加新字段到 summary_table
    new_summary_fields = [
        ('岗位工资', 'DECIMAL(10,2)'),
        ('交通费', 'DECIMAL(10,2)'),
        ('手机费', 'DECIMAL(10,2)'),
        ('奖金', 'DECIMAL(10,2)'),
        ('高温费', 'DECIMAL(10,2)'),
        ('其它捐款', 'DECIMAL(10,2)'),
        ('住宿捐款', 'DECIMAL(10,2)'),
        ('水电捐款', 'DECIMAL(10,2)')
    ]
    
    for field_name, field_type in new_summary_fields:
        try:
            cursor.execute(f"ALTER TABLE summary_table ADD COLUMN {field_name} {field_type}")
            print(f"  summary_table.{field_name} 添加成功")
        except Exception as e:
            if "duplicate column name" in str(e).lower():
                print(f"  summary_table.{field_name} 已存在")
            else:
                print(f"  summary_table.{field_name} 添加失败: {e}")
    
    conn.commit()
    conn.close()
    
    print("数据库迁移完成！")

if __name__ == '__main__':
    migrate()
