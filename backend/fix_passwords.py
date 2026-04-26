#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
修复用户密码 - 将明文密码转换为哈希格式
使用方法: python fix_passwords.py
"""

import sqlite3
import os
from werkzeug.security import generate_password_hash, check_password_hash

DATABASE_PATH = os.path.join(os.path.dirname(__file__), '..', 'database', 'salary_system.db')

def is_hashed_password(password):
    """检查密码是否已经是哈希格式"""
    if not password:
        return False
    return password.startswith('pbkdf2:sha256:') or password.startswith('sha256:')

def fix_passwords():
    conn = sqlite3.connect(DATABASE_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    
    cursor.execute("SELECT 姓名, 密码, role FROM users")
    users = cursor.fetchall()
    
    print("=" * 60)
    print("用户密码检查与修复")
    print("=" * 60)
    
    fixed_count = 0
    
    for user in users:
        name = user['姓名']
        password = user['密码']
        role = user['role']
        
        print(f"\n用户: {name} (角色: {role})")
        
        if not password:
            print(f"  ⚠ 密码为空，跳过")
            continue
        
        if is_hashed_password(password):
            print(f"  ✓ 密码已是哈希格式")
            continue
        
        print(f"  ⚠ 密码是明文: {password}")
        
        hashed_password = generate_password_hash(password, method='pbkdf2:sha256:10000')
        
        cursor.execute(
            "UPDATE users SET 密码 = ? WHERE 姓名 = ?",
            (hashed_password, name)
        )
        
        print(f"  ✓ 已转换为哈希格式")
        fixed_count += 1
    
    conn.commit()
    conn.close()
    
    print("\n" + "=" * 60)
    print(f"修复完成! 共修复 {fixed_count} 个用户密码")
    print("=" * 60)

if __name__ == '__main__':
    fix_passwords()
