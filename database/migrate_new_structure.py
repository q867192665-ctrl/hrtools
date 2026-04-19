#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
数据库重构脚本
按照新的汇总表头重新创建表结构
"""

import sqlite3
import os

DATABASE_PATH = 'e:/22/database/salary_system.db'
BACKUP_PATH = 'e:/22/database/salary_system_backup.db'

def migrate_database():
    conn = sqlite3.connect(DATABASE_PATH)
    cursor = conn.cursor()
    
    print("开始数据库重构...")
    
    # 1. 备份现有数据
    print("1. 备份现有数据...")
    
    # 备份用户数据
    cursor.execute("SELECT 姓名, 密码, role, token, token_expire_time, last_login_time, created_at FROM users")
    users_data = cursor.fetchall()
    print(f"   用户数据: {len(users_data)} 条")
    
    # 备份签名文件数据
    cursor.execute("SELECT 文件ID, 用户姓名, 文件名, 存储路径, 文件格式, 文件大小, 上传时间, 文件状态, 签收状态, 重签原因, 签收时间 FROM signature_files")
    signatures_data = cursor.fetchall()
    print(f"   签名文件数据: {len(signatures_data)} 条")
    
    # 2. 删除旧表
    print("2. 删除旧表...")
    cursor.execute("DROP TABLE IF EXISTS salary_table")
    cursor.execute("DROP TABLE IF EXISTS summary_table")
    cursor.execute("DROP TABLE IF EXISTS users")
    cursor.execute("DROP TABLE IF EXISTS signature_files")
    
    # 3. 创建新表
    print("3. 创建新表...")
    
    # 用户表（添加手机号字段）
    cursor.execute("""
        CREATE TABLE users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            姓名 TEXT NOT NULL UNIQUE,
            密码 TEXT NOT NULL,
            手机号 TEXT,
            role TEXT CHECK(role IN ('user', 'admin')) DEFAULT 'user',
            token TEXT,
            token_expire_time TIMESTAMP,
            last_login_time TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    print("   用户表创建完成")
    
    # 工资表
    cursor.execute("""
        CREATE TABLE salary_table (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            序号 TEXT,
            部门 TEXT,
            姓名 TEXT NOT NULL UNIQUE,
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
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    print("   工资表创建完成")
    
    # 汇总表
    cursor.execute("""
        CREATE TABLE summary_table (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            序号 TEXT,
            部门 TEXT,
            姓名 TEXT NOT NULL,
            入职日期 DATE,
            手机号 TEXT,
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
            查询状态 TEXT DEFAULT '未查询',
            查询次数 INTEGER DEFAULT 0,
            签收状态 TEXT DEFAULT '未签收',
            最新签收时间 TIMESTAMP,
            签收方式 TEXT,
            签名图片 TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    print("   汇总表创建完成")
    
    # 签名文件表
    cursor.execute("""
        CREATE TABLE signature_files (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            文件ID TEXT NOT NULL UNIQUE,
            用户姓名 TEXT NOT NULL,
            文件名 TEXT NOT NULL,
            存储路径 TEXT NOT NULL,
            文件格式 TEXT,
            文件大小 INTEGER,
            上传时间 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            文件状态 TEXT DEFAULT '正常',
            签收状态 TEXT DEFAULT '未签收',
            重签原因 TEXT,
            签收时间 TIMESTAMP
        )
    """)
    print("   签名文件表创建完成")
    
    # 4. 恢复用户数据
    print("4. 恢复用户数据...")
    for user in users_data:
        try:
            cursor.execute(
                """INSERT INTO users (姓名, 密码, role, token, token_expire_time, last_login_time, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?)""",
                user
            )
        except Exception as e:
            print(f"   恢复用户 {user[0]} 失败: {e}")
    print(f"   用户数据恢复完成")
    
    # 5. 恢复签名文件数据
    print("5. 恢复签名文件数据...")
    for sig in signatures_data:
        try:
            cursor.execute(
                """INSERT INTO signature_files 
                   (文件ID, 用户姓名, 文件名, 存储路径, 文件格式, 文件大小, 上传时间, 文件状态, 签收状态, 重签原因, 签收时间)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                sig
            )
        except Exception as e:
            print(f"   恢复签名文件 {sig[0]} 失败: {e}")
    print(f"   签名文件数据恢复完成")
    
    # 6. 创建索引
    print("6. 创建索引...")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_users_name ON users(姓名)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_users_phone ON users(手机号)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_salary_name ON salary_table(姓名)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_summary_name ON summary_table(姓名)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_signature_file_id ON signature_files(文件ID)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_signature_user ON signature_files(用户姓名)")
    print("   索引创建完成")
    
    # 7. 插入默认管理员账号
    print("7. 确保管理员账号存在...")
    cursor.execute("INSERT OR IGNORE INTO users (姓名, 密码, role) VALUES ('admin', 'admin123', 'admin')")
    
    conn.commit()
    conn.close()
    
    print("\n数据库重构完成！")
    print("新表结构：")
    print("  - users: 姓名, 密码, 手机号, role, token...")
    print("  - salary_table: 序号, 部门, 姓名, 入职日期, 手机号, 是否代扣社保, 岗位, 应出勤天数...")
    print("  - summary_table: 序号, 部门, 姓名, 入职日期, 手机号, 是否代扣社保, 岗位... + 查询状态, 签收状态, 签名图片")
    print("  - signature_files: 文件ID, 用户姓名, 文件名...")

if __name__ == '__main__':
    migrate_database()
