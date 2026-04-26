#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
容器内更新脚本 - 从GitHub拉取最新代码并更新程序
使用方法: python update_from_github.py
"""

import os
import sys
import subprocess
import sqlite3

GITHUB_REPO = "https://github.com/q867192665-ctrl/hrtools.git"
APP_DIR = "/app"
DATABASE_PATH = "/app/database/salary_system.db"

def run_command(cmd, cwd=None):
    """执行命令并返回结果"""
    print(f"\n执行: {cmd}")
    result = subprocess.run(cmd, shell=True, cwd=cwd, capture_output=True, text=True)
    if result.stdout:
        print(result.stdout)
    if result.returncode != 0:
        print(f"错误: {result.stderr}")
    return result.returncode == 0

def check_git():
    """检查是否是git仓库"""
    if os.path.exists(os.path.join(APP_DIR, ".git")):
        return True
    return False

def update_code():
    """从GitHub更新代码"""
    print("=" * 60)
    print("步骤 1: 更新代码")
    print("=" * 60)
    
    if check_git():
        print("检测到Git仓库，执行 git pull...")
        return run_command("git pull origin master", cwd=APP_DIR)
    else:
        print("未检测到Git仓库，尝试克隆...")
        return run_command(f"git clone {GITHUB_REPO} .", cwd=APP_DIR)

def update_database():
    """更新数据库表结构"""
    print("\n" + "=" * 60)
    print("步骤 2: 更新数据库")
    print("=" * 60)
    
    if not os.path.exists(DATABASE_PATH):
        print(f"警告: 数据库文件不存在: {DATABASE_PATH}")
        return False
    
    try:
        conn = sqlite3.connect(DATABASE_PATH)
        cursor = conn.cursor()
        
        print("检查 app_versions 表...")
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='app_versions'")
        
        if cursor.fetchone():
            print("app_versions 表已存在")
        else:
            print("创建 app_versions 表...")
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
        return True
        
    except Exception as e:
        print(f"数据库更新失败: {e}")
        return False

def install_dependencies():
    """安装Python依赖"""
    print("\n" + "=" * 60)
    print("步骤 3: 安装依赖")
    print("=" * 60)
    
    requirements_path = os.path.join(APP_DIR, "backend", "requirements.txt")
    if os.path.exists(requirements_path):
        print("安装Python依赖...")
        return run_command(f"pip install -r {requirements_path}")
    else:
        print("未找到 requirements.txt，跳过")
        return True

def restart_services():
    """重启服务（如果使用supervisor或systemd）"""
    print("\n" + "=" * 60)
    print("步骤 4: 重启服务")
    print("=" * 60)
    
    # 尝试不同的重启方式
    restart_commands = [
        "supervisorctl restart all",
        "systemctl restart gunicorn",
        "service gunicorn restart",
    ]
    
    for cmd in restart_commands:
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        if result.returncode == 0:
            print(f"服务重启成功: {cmd}")
            return True
    
    print("提示: 请手动重启服务，或服务可能已自动重载")
    return True

def main():
    print("\n" + "=" * 60)
    print("容器内更新脚本")
    print("=" * 60)
    print(f"应用目录: {APP_DIR}")
    print(f"数据库路径: {DATABASE_PATH}")
    print(f"GitHub仓库: {GITHUB_REPO}")
    
    steps = [
        ("更新代码", update_code),
        ("更新数据库", update_database),
        ("安装依赖", install_dependencies),
        ("重启服务", restart_services),
    ]
    
    results = []
    for name, func in steps:
        try:
            success = func()
            results.append((name, "成功" if success else "失败"))
        except Exception as e:
            print(f"\n{name}时发生错误: {e}")
            results.append((name, f"错误: {e}"))
    
    print("\n" + "=" * 60)
    print("更新结果汇总")
    print("=" * 60)
    for name, status in results:
        print(f"  {name}: {status}")
    
    print("\n" + "=" * 60)
    print("更新完成!")
    print("=" * 60)

if __name__ == "__main__":
    main()
