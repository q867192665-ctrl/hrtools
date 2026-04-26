#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据库更新脚本 - 创建 app_versions 表
使用方法: python update_db_app_versions.py
"""

import sqlite3
import os

DATABASE_PATH = os.path.join(os.path.dirname(__file__), 'database', 'salary_system.db')

def main():
    print("=" * 60)
    print("数据库更新脚本 - 创建 app_versions 表")
    print("=" * 60)
    
    if not os.path.exists(DATABASE_PATH):
        print(f"错误: 数据库文件不存在: {DATABASE_PATH}")
        return False
    
    print(f"\n数据库路径: {DATABASE_PATH}")
    print(f"数据库大小: {os.path.getsize(DATABASE_PATH)} 字节")
    
    try:
        conn = sqlite3.connect(DATABASE_PATH)
        cursor = conn.cursor()
        
        print("\n检查 app_versions 表是否存在...")
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='app_versions'")
        
        if cursor.fetchone():
            print("app_versions 表已存在，无需创建。")
            
            print("\n当前表中的记录:")
            cursor.execute("SELECT id, version, version_name, is_active, upload_time FROM app_versions ORDER BY upload_time DESC LIMIT 5")
            rows = cursor.fetchall()
            
            if rows:
                print(f"{'ID':<5} {'版本':<10} {'版本名':<10} {'激活':<6} {'上传时间'}")
                print("-" * 60)
                for row in rows:
                    print(f"{row[0]:<5} {row[1]:<10} {row[2] or 'N/A':<10} {row[3]:<6} {row[4] or 'N/A'}")
            else:
                print("表中暂无记录。")
        else:
            print("app_versions 表不存在，正在创建...")
            
            cursor.execute('''
                CREATE TABLE app_versions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    version TEXT NOT NULL,
                    version_name TEXT,
                    file_name TEXT NOT NULL,
                    file_path TEXT NOT NULL,
                    file_size INTEGER,
                    update_note TEXT,
                    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    is_active INTEGER DEFAULT 1
                )
            ''')
            
            conn.commit()
            print("app_versions 表创建成功!")
        
        conn.close()
        print("\n" + "=" * 60)
        print("更新完成!")
        print("=" * 60)
        return True
        
    except Exception as e:
        print(f"\n错误: {e}")
        return False

if __name__ == "__main__":
    main()
