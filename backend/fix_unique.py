#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""修复 salary_table 唯一约束"""

import sqlite3

DATABASE_PATH = 'E:/22/database/salary_system.db'

def fix():
    conn = sqlite3.connect(DATABASE_PATH)
    cursor = conn.cursor()
    
    print("步骤1: 查看当前数据...")
    cursor.execute("SELECT COUNT(*) FROM salary_table")
    print(f"  当前记录数: {cursor.fetchone()[0]}")
    
    print("\n步骤2: 创建新表(无姓名唯一约束)...")
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS salary_table_new (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            序号 TEXT,
            部门 TEXT,
            姓名 TEXT,
            入职日期 DATE,
            是否代扣社保 BOOLEAN,
            岗位 TEXT,
            应出勤天数 DECIMAL(4,1),
            实际出勤天数 DECIMAL(4,1),
            上门服务小时 INTEGER,
            基本工资底薪 DECIMAL(10,2),
            基本工资其它补贴 DECIMAL(10,2),
            基本工资合计 DECIMAL(10,2),
            应扣款项缺勤扣款 DECIMAL(10,2),
            应扣款项养老 DECIMAL(10,2),
            应扣款项医疗 DECIMAL(10,2),
            应扣款项失业 DECIMAL(10,2),
            应扣款项公积金 DECIMAL(10,2),
            应扣款项应缴个税 DECIMAL(10,2),
            实发工资 DECIMAL(10,2),
            护理员绩效工资 DECIMAL(10,2),
            应发工资 DECIMAL(10,2),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP,
            岗位工资 DECIMAL(10,2),
            交通费 DECIMAL(10,2),
            手机费 DECIMAL(10,2),
            奖金 DECIMAL(10,2),
            高温费 DECIMAL(10,2),
            其它扣款 DECIMAL(10,2),
            住宿扣款 DECIMAL(10,2),
            水电扣款 DECIMAL(10,2),
            月份 TEXT
        )
    """)
    print("  新表创建成功")
    
    print("\n步骤3: 迁移数据...")
    cursor.execute("INSERT INTO salary_table_new SELECT * FROM salary_table")
    migrated = cursor.rowcount
    print(f"  迁移了 {migrated} 条记录")
    
    print("\n步骤4: 删除旧表...")
    cursor.execute("DROP TABLE salary_table")
    print("  旧表已删除")
    
    print("\n步骤5: 重命名新表...")
    cursor.execute("ALTER TABLE salary_table_new RENAME TO salary_table")
    print("  重命名完成")
    
    print("\n步骤6: 验证结果...")
    cursor.execute("PRAGMA table_info(salary_table)")
    for col in cursor.fetchall():
        unique = " [UNIQUE]" if col[3] != 0 else ""
        if unique:
            print(f"  {col[1]:20s}{unique}")
    
    cursor.execute("SELECT COUNT(*) FROM salary_table")
    print(f"\n  最终记录数: {cursor.fetchone()[0]}")
    
    conn.commit()
    conn.close()
    
    print("\n✅ 修复完成！姓名字段的唯一约束已移除")

if __name__ == "__main__":
    fix()