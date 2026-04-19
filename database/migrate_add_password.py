#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
数据库迁移脚本：添加密码字段
功能：给users表添加密码字段，并为现有用户设置默认密码
"""

import sqlite3
import os
from werkzeug.security import generate_password_hash

def migrate_database():
    db_path = 'salary_system.db'
    
    if not os.path.exists(db_path):
        print(f"[ERROR] 数据库文件不存在: {db_path}")
        return False
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        print("=" * 60)
        print("[INFO] 数据库迁移：添加密码字段")
        print("=" * 60)
        
        # 检查密码字段是否已存在
        cursor.execute("PRAGMA table_info(users)")
        columns = cursor.fetchall()
        has_password = any(col[1] == '密码' for col in columns)
        
        if has_password:
            print("[INFO] 密码字段已存在，跳过迁移")
            conn.close()
            return True
        
        # 添加密码字段
        print("\n[INFO] 添加密码字段...")
        cursor.execute("ALTER TABLE users ADD COLUMN 密码 TEXT")
        
        # 为现有用户设置默认密码
        print("[INFO] 为现有用户设置默认密码...")
        
        # 获取所有用户
        cursor.execute("SELECT 姓名, role FROM users")
        users = cursor.fetchall()
        
        default_passwords = {
            'admin': 'admin123',
            '张三': '123456',
            '李四': '123456',
            '王五': '123456'
        }
        
        for user_name, role in users:
            # 设置默认密码（如果用户在默认密码列表中）
            password = default_passwords.get(user_name, '123456')
            hashed_password = generate_password_hash(password)
            
            cursor.execute(
                "UPDATE users SET 密码 = ? WHERE 姓名 = ?",
                (hashed_password, user_name)
            )
            print(f"  - {user_name}: {password}")
        
        conn.commit()
        
        # 验证迁移结果
        print("\n[INFO] 验证迁移结果...")
        cursor.execute("PRAGMA table_info(users)")
        columns = cursor.fetchall()
        
        print("\nusers表字段:")
        for col in columns:
            print(f"  - {col[1]} ({col[2]})")
        
        # 查询用户数据
        print("\n用户数据:")
        cursor.execute("SELECT 姓名, 密码, role FROM users")
        users_data = cursor.fetchall()
        
        for name, pwd, role in users_data:
            print(f"  - {name} ({role}): 密码已设置")
        
        conn.close()
        
        print("\n" + "=" * 60)
        print("[SUCCESS] 数据库迁移完成！")
        print("=" * 60)
        
        print("\n默认密码:")
        print("  - admin: admin123")
        print("  - 其他用户: 123456")
        
        return True
        
    except Exception as e:
        print(f"[ERROR] 迁移失败: {e}")
        return False


if __name__ == '__main__':
    migrate_database()
