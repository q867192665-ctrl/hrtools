#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""检查管理员账号"""

import sqlite3
import os

DATABASE_PATH = os.path.join(os.path.dirname(__file__), '..', 'database', 'salary_system.db')

def check_admin_account():
    """检查管理员账号是否存在"""
    try:
        conn = sqlite3.connect(DATABASE_PATH)
        cursor = conn.cursor()
        
        # 检查admin账号
        cursor.execute("SELECT 姓名, 密码, role FROM users WHERE 姓名='admin'")
        admin_user = cursor.fetchone()
        
        if admin_user:
            print("✅ 管理员账号存在:")
            print(f"   用户名: {admin_user[0]}")
            print(f"   密码哈希: {admin_user[1]}")
            print(f"   角色: {admin_user[2]}")
        else:
            print("❌ 管理员账号不存在")
            print("正在创建管理员账号...")
            
            from werkzeug.security import generate_password_hash
            password_hash = generate_password_hash('admin123', method='pbkdf2:sha256:10000')
            cursor.execute("INSERT INTO users (姓名, 密码, role, created_at) VALUES (?, ?, ?, datetime('now'))", 
                          ('admin', password_hash, 'admin'))
            conn.commit()
            print("✅ 管理员账号创建成功")
            print(f"   用户名: admin")
            print(f"   密码: admin123")
        
        cursor.execute("SELECT 姓名, role FROM users")
        all_users = cursor.fetchall()
        print(f"\n📊 系统中共有 {len(all_users)} 个用户:")
        for user in all_users:
            print(f"   {user[0]} - {user[1]}")
            
    except Exception as e:
        print(f"❌ 检查失败: {e}")
    finally:
        if conn:
            conn.close()

if __name__ == "__main__":
    check_admin_account()