#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""验证数据库表结构"""

import sqlite3

def verify_database():
    db_path = 'salary_system.db'
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # 查询所有表
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")
        tables = cursor.fetchall()
        
        print("=" * 60)
        print("[INFO] 数据库表结构验证")
        print("=" * 60)
        print(f"\n数据库文件: {db_path}")
        print(f"表总数: {len(tables)}")
        print("\n表列表:")
        print("-" * 60)
        
        for table in tables:
            table_name = table[0]
            print(f"\n[{table_name}]")
            
            # 查询表结构
            cursor.execute(f"PRAGMA table_info({table_name})")
            columns = cursor.fetchall()
            
            print(f"  字段数: {len(columns)}")
            print("  字段列表:")
            
            for col in columns:
                col_id, col_name, col_type, not_null, default_val, pk = col
                constraints = []
                if pk:
                    constraints.append("PRIMARY KEY")
                if not_null:
                    constraints.append("NOT NULL")
                if default_val:
                    constraints.append(f"DEFAULT {default_val}")
                
                constraint_str = " ".join(constraints) if constraints else ""
                print(f"    - {col_name} ({col_type}) {constraint_str}")
            
            # 查询索引
            cursor.execute(f"PRAGMA index_list({table_name})")
            indexes = cursor.fetchall()
            
            if indexes:
                print("  索引:")
                for idx in indexes:
                    idx_name = idx[1]
                    cursor.execute(f"PRAGMA index_info({idx_name})")
                    idx_cols = cursor.fetchall()
                    col_names = [col[2] for col in idx_cols]
                    print(f"    - {idx_name}: {', '.join(col_names)}")
        
        # 验证signature_files表
        print("\n" + "=" * 60)
        print("[INFO] signature_files表详细验证")
        print("=" * 60)
        
        cursor.execute("PRAGMA table_info(signature_files)")
        sig_cols = cursor.fetchall()
        
        expected_cols = [
            ('id', 'INTEGER'),
            ('文件ID', 'TEXT'),
            ('用户姓名', 'TEXT'),
            ('文件名', 'TEXT'),
            ('存储路径', 'TEXT'),
            ('文件格式', 'TEXT'),
            ('文件大小', 'INTEGER'),
            ('上传时间', 'TIMESTAMP'),
            ('文件状态', 'TEXT')
        ]
        
        print("\n字段验证:")
        all_ok = True
        for exp_name, exp_type in expected_cols:
            found = False
            for col in sig_cols:
                if col[1] == exp_name:
                    found = True
                    if col[2] == exp_type:
                        print(f"  [OK] {exp_name} ({exp_type})")
                    else:
                        print(f"  [WARN] {exp_name}: 期望类型 {exp_type}, 实际类型 {col[2]}")
                        all_ok = False
                    break
            
            if not found:
                print(f"  [ERROR] 缺少字段: {exp_name}")
                all_ok = False
        
        if all_ok:
            print("\n[SUCCESS] signature_files表结构验证通过！")
        else:
            print("\n[ERROR] signature_files表结构验证失败！")
        
        conn.close()
        
        print("\n" + "=" * 60)
        print("[OK] 数据库验证完成")
        print("=" * 60)
        
    except Exception as e:
        print(f"[ERROR] 验证失败: {e}")

if __name__ == '__main__':
    verify_database()
