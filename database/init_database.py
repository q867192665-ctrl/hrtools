#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
工资系统数据库初始化脚本
功能：创建数据库表结构，初始化基础数据
"""

import sqlite3
import os
from datetime import datetime
from werkzeug.security import generate_password_hash

class DatabaseInitializer:
    def __init__(self, db_path='salary_system.db'):
        self.db_path = db_path
        self.conn = None
        self.cursor = None
    
    def connect(self):
        """连接数据库"""
        try:
            self.conn = sqlite3.connect(self.db_path)
            self.cursor = self.conn.cursor()
            print(f"[OK] 成功连接到数据库: {self.db_path}")
            return True
        except Exception as e:
            print(f"[ERROR] 连接数据库失败: {e}")
            return False
    
    def execute_sql_file(self, sql_file):
        """执行SQL文件"""
        try:
            with open(sql_file, 'r', encoding='utf-8') as f:
                sql_script = f.read()
            
            # 分割SQL语句（按分号分割，但忽略注释中的分号）
            statements = []
            current_statement = []
            in_comment = False
            
            for line in sql_script.split('\n'):
                line = line.strip()
                
                # 跳过空行和注释
                if not line or line.startswith('--'):
                    continue
                
                # 处理多行注释
                if '/*' in line:
                    in_comment = True
                if '*/' in line:
                    in_comment = False
                    continue
                if in_comment:
                    continue
                
                current_statement.append(line)
                
                # 如果行以分号结束，表示一个完整的SQL语句
                if line.endswith(';'):
                    statements.append(' '.join(current_statement))
                    current_statement = []
            
            # 执行所有SQL语句
            success_count = 0
            error_count = 0
            
            for statement in statements:
                try:
                    self.cursor.execute(statement)
                    success_count += 1
                except Exception as e:
                    # 忽略"表已存在"的错误
                    if 'already exists' not in str(e).lower():
                        print(f"[WARN] 执行SQL语句失败: {str(e)[:100]}")
                        error_count += 1
            
            self.conn.commit()
            print(f"[OK] SQL文件执行完成: 成功 {success_count} 条, 失败 {error_count} 条")
            return True
            
        except Exception as e:
            print(f"[ERROR] 执行SQL文件失败: {e}")
            return False
    
    def verify_tables(self):
        """验证表是否创建成功"""
        try:
            # 查询所有表
            self.cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
            tables = self.cursor.fetchall()
            
            print("\n[INFO] 数据库表列表:")
            print("-" * 50)
            
            expected_tables = [
                'salary_table',
                'summary_table', 
                'users',
                'signature_files',
                'operation_logs',
                'backup_records'
            ]
            
            for table_name in expected_tables:
                found = any(table_name in str(table) for table in tables)
                status = "[OK]" if found else "[ERROR]"
                print(f"{status} {table_name}")
            
            # 查询视图
            self.cursor.execute("SELECT name FROM sqlite_master WHERE type='view'")
            views = self.cursor.fetchall()
            
            if views:
                print("\n[INFO] 数据库视图:")
                print("-" * 50)
                for view in views:
                    print(f"[OK] {view[0]}")
            
            return True
            
        except Exception as e:
            print(f"[ERROR] 验证表失败: {e}")
            return False
    
    def insert_sample_data(self):
        """插入示例数据"""
        try:
            print("\n[INFO] 插入示例数据...")
            
            sample_users = [
                ('admin', 'admin123', 'admin', None, None, None),
                ('张三', '123456', 'user', '护理部', None, '2023-01-15'),
                ('李四', '123456', 'user', '护理部', None, '2023-03-20'),
                ('王五', '123456', 'user', '后勤部', None, '2023-06-10')
            ]
            
            for name, password, role, dept, phone, hire_date in sample_users:
                try:
                    hashed = generate_password_hash(password, method='pbkdf2:sha256:10000')
                    self.cursor.execute(
                        "INSERT OR IGNORE INTO users (姓名, 密码, role, 部门, 手机号, 入职日期) VALUES (?, ?, ?, ?, ?, ?)",
                        (name, hashed, role, dept, phone, hire_date)
                    )
                except:
                    pass
            
            # 插入示例工资数据
            sample_salaries = [
                ('001', '护理部', '张三', '护理员', '2023-01-15', 22.0, 21.5, 15,
                 3000.00, 500.00, 3500.00, 1000.00, 800.00, 5300.00,
                 300.00, 200.00, 50.00, 0.00, 550.00, 4750.00, True, '', '2026-04'),
                ('002', '护理部', '李四', '护理员', '2023-03-20', 22.0, 22.0, 20,
                 3200.00, 600.00, 3800.00, 1200.00, 900.00, 5900.00,
                 320.00, 240.00, 60.00, 0.00, 620.00, 5280.00, True, '', '2026-04'),
                ('003', '后勤部', '王五', '后勤员', '2023-06-10', 22.0, 20.0, 0,
                 2800.00, 400.00, 3200.00, 800.00, 600.00, 4600.00,
                 280.00, 180.00, 40.00, 0.00, 500.00, 4100.00, False, '', '2026-04')
            ]
            
            for salary in sample_salaries:
                try:
                    self.cursor.execute(
                        """INSERT OR IGNORE INTO salary_table 
                        (序号, 部门, 姓名, 岗位, 入职日期, 应出勤天数, 实际出勤天数, 上门服务小时,
                         基本工资底薪, 基本工资绩效, 基本工资合计, 岗位工资, 护理员绩效工资, 应发工资,
                         应扣款项社保, 应扣款项公积金, 应扣款项个税, 应扣款项其他, 应扣款项合计, 
                         实发工资, 是否代扣社保, 备注, 月份)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                        salary
                    )
                except:
                    pass
            
            self.conn.commit()
            print("[OK] 示例数据插入完成")
            return True
            
        except Exception as e:
            print(f"[ERROR] 插入示例数据失败: {e}")
            return False
    
    def close(self):
        """关闭数据库连接"""
        if self.conn:
            self.conn.close()
            print("\n[OK] 数据库连接已关闭")
    
    def initialize(self, sql_file='schema.sql', insert_sample=True):
        """初始化数据库"""
        print("=" * 60)
        print("[INFO] 工资系统数据库初始化")
        print("=" * 60)
        
        # 连接数据库
        if not self.connect():
            return False
        
        # 执行SQL文件
        if not self.execute_sql_file(sql_file):
            return False
        
        # 验证表
        if not self.verify_tables():
            return False
        
        # 插入示例数据
        if insert_sample:
            if not self.insert_sample_data():
                return False
        
        # 关闭连接
        self.close()
        
        print("\n" + "=" * 60)
        print("[OK] 数据库初始化完成！")
        print("=" * 60)
        
        return True


def main():
    """主函数"""
    # 获取脚本所在目录
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    # SQL文件路径
    sql_file = os.path.join(script_dir, 'schema.sql')
    
    # 数据库文件路径
    db_file = os.path.join(script_dir, 'salary_system.db')
    
    # 创建初始化器
    initializer = DatabaseInitializer(db_file)
    
    # 执行初始化
    success = initializer.initialize(sql_file, insert_sample=True)
    
    if success:
        print("\n[SUCCESS] 数据库初始化成功！")
        print(f"[INFO] 数据库文件: {db_file}")
        print("\n下一步:")
        print("1. 查看数据库表结构")
        print("2. 开始开发后端API")
        print("3. 测试数据库连接")
    else:
        print("\n[ERROR] 数据库初始化失败，请检查错误信息")


if __name__ == '__main__':
    main()
