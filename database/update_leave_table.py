#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""更新请假记录表 - 移除审批相关字段"""

import sqlite3
import os

DATABASE_PATH = os.path.join(os.path.dirname(__file__), 'salary_system.db')

def update_leave_table():
    """更新请假记录表结构"""
    try:
        conn = sqlite3.connect(DATABASE_PATH)
        cursor = conn.cursor()
        
        # 检查表是否存在
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='leave_records'")
        if cursor.fetchone():
            # 备份旧数据
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS leave_records_backup AS
                SELECT id, 姓名, 请假类型, 开始日期, 结束日期, 请假天数, 请假原因, 申请时间
                FROM leave_records
            """)
            
            # 删除旧表
            cursor.execute("DROP TABLE leave_records")
            
            # 创建新表
            cursor.execute("""
                CREATE TABLE leave_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    姓名 TEXT NOT NULL,
                    请假类型 TEXT NOT NULL,
                    开始日期 TEXT NOT NULL,
                    结束日期 TEXT NOT NULL,
                    请假天数 REAL NOT NULL,
                    请假原因 TEXT,
                    申请时间 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (姓名) REFERENCES users(姓名)
                )
            """)
            
            # 恢复数据
            cursor.execute("""
                INSERT INTO leave_records (id, 姓名, 请假类型, 开始日期, 结束日期, 请假天数, 请假原因, 申请时间)
                SELECT id, 姓名, 请假类型, 开始日期, 结束日期, 请假天数, 请假原因, 申请时间
                FROM leave_records_backup
            """)
            
            # 删除备份表
            cursor.execute("DROP TABLE leave_records_backup")
            
            # 重新创建索引
            cursor.execute("CREATE INDEX IF NOT EXISTS idx_leave_name ON leave_records(姓名)")
            cursor.execute("CREATE INDEX IF NOT EXISTS idx_leave_dates ON leave_records(开始日期, 结束日期)")
            
            conn.commit()
            print("请假记录表更新成功")
        else:
            # 表不存在，直接创建
            cursor.execute("""
                CREATE TABLE leave_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    姓名 TEXT NOT NULL,
                    请假类型 TEXT NOT NULL,
                    开始日期 TEXT NOT NULL,
                    结束日期 TEXT NOT NULL,
                    请假天数 REAL NOT NULL,
                    请假原因 TEXT,
                    申请时间 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (姓名) REFERENCES users(姓名)
                )
            """)
            
            cursor.execute("CREATE INDEX IF NOT EXISTS idx_leave_name ON leave_records(姓名)")
            cursor.execute("CREATE INDEX IF NOT EXISTS idx_leave_dates ON leave_records(开始日期, 结束日期)")
            
            conn.commit()
            print("请假记录表创建成功")
        
    except Exception as e:
        print(f"更新请假记录表失败: {e}")
    finally:
        if conn:
            conn.close()

if __name__ == '__main__':
    update_leave_table()
