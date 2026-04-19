#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""创建请假记录表"""

import sqlite3
import os

DATABASE_PATH = os.path.join(os.path.dirname(__file__), 'salary_system.db')

def create_leave_table():
    """创建请假记录表"""
    try:
        conn = sqlite3.connect(DATABASE_PATH)
        cursor = conn.cursor()
        
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS leave_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                姓名 TEXT NOT NULL,
                请假类型 TEXT NOT NULL,
                开始日期 TEXT NOT NULL,
                结束日期 TEXT NOT NULL,
                请假天数 REAL NOT NULL,
                请假原因 TEXT,
                申请时间 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                审批状态 TEXT DEFAULT '待审批',
                审批人 TEXT,
                审批时间 TIMESTAMP,
                审批意见 TEXT,
                FOREIGN KEY (姓名) REFERENCES users(姓名)
            )
        """)
        
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_leave_name ON leave_records(姓名)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_leave_status ON leave_records(审批状态)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_leave_dates ON leave_records(开始日期, 结束日期)")
        
        conn.commit()
        print("请假记录表创建成功")
        
    except Exception as e:
        print(f"创建请假记录表失败: {e}")
    finally:
        if conn:
            conn.close()

if __name__ == '__main__':
    create_leave_table()
