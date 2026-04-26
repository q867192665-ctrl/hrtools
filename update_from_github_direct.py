#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
容器内增量更新脚本 - 仅下载有变化的文件（无需git）
使用方法: python3 update_from_github_direct.py
"""

import os
import sys
import urllib.request
import json
import sqlite3
import hashlib

GITHUB_REPO = "q867192665-ctrl/hrtools"
GITHUB_BRANCH = "master"
APP_DIR = "/app"
DATABASE_PATH = "/app/database/salary_system.db"

def get_github_raw_url(file_path):
    """获取GitHub raw文件URL"""
    return f"https://raw.githubusercontent.com/{GITHUB_REPO}/{GITHUB_BRANCH}/{file_path}"

def get_file_hash(file_path):
    """计算文件SHA256哈希"""
    sha256 = hashlib.sha256()
    try:
        with open(file_path, 'rb') as f:
            for chunk in iter(lambda: f.read(8192), b''):
                sha256.update(chunk)
        return sha256.hexdigest()
    except:
        return None

def download_file(url, save_path):
    """下载文件并保存"""
    print(f"  下载: {url}")
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=30) as response:
            content = response.read()
        
        os.makedirs(os.path.dirname(save_path), exist_ok=True)
        with open(save_path, 'wb') as f:
            f.write(content)
        
        print(f"  保存: {save_path} ({len(content)} bytes)")
        return True
    except Exception as e:
        print(f"  下载失败: {e}")
        return False

def get_file_list_from_github():
    """从GitHub获取文件列表和SHA"""
    print("=" * 60)
    print("步骤 1: 获取GitHub文件列表")
    print("=" * 60)
    
    url = f"https://api.github.com/repos/{GITHUB_REPO}/git/trees/{GITHUB_BRANCH}?recursive=1"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=30) as response:
            data = json.loads(response.read().decode())
        
        files = {}
        for item in data.get('tree', []):
            if item['type'] == 'blob':
                files[item['path']] = item['sha']
        
        print(f"获取到 {len(files)} 个文件")
        return files
    except Exception as e:
        print(f"获取文件列表失败: {e}")
        return {}

def check_files_to_update(github_files):
    """检查哪些文件需要更新"""
    print("\n" + "=" * 60)
    print("步骤 2: 检查需要更新的文件")
    print("=" * 60)
    
    files_to_update = []
    skip_count = 0
    
    for file_path, github_sha in github_files.items():
        if file_path.startswith('database/'):
            skip_count += 1
            continue
        
        local_path = os.path.join(APP_DIR, file_path)
        
        if not os.path.exists(local_path):
            print(f"  [新增] {file_path}")
            files_to_update.append(file_path)
            continue
        
        local_hash = get_file_hash(local_path)
        
        if local_hash != github_sha:
            print(f"  [更新] {file_path}")
            files_to_update.append(file_path)
        else:
            skip_count += 1
    
    print(f"\n需要更新: {len(files_to_update)} 个文件, 跳过: {skip_count} 个文件")
    return files_to_update

def update_files(files_to_update):
    """下载并更新文件"""
    if not files_to_update:
        print("\n所有文件都是最新的，无需更新")
        return True
    
    print("\n" + "=" * 60)
    print("步骤 3: 下载更新文件")
    print("=" * 60)
    
    success_count = 0
    fail_count = 0
    
    for file_path in files_to_update:
        save_path = os.path.join(APP_DIR, file_path)
        url = get_github_raw_url(file_path)
        
        if download_file(url, save_path):
            success_count += 1
        else:
            fail_count += 1
    
    print(f"\n下载完成: 成功 {success_count} 个, 失败 {fail_count} 个")
    return fail_count == 0

def update_database():
    """更新数据库表结构"""
    print("\n" + "=" * 60)
    print("步骤 4: 更新数据库")
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

def restart_services():
    """重启服务"""
    print("\n" + "=" * 60)
    print("步骤 5: 重启服务")
    print("=" * 60)
    
    import subprocess
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
    
    print("提示: 请手动重启容器或服务")
    return True

def main():
    print("\n" + "=" * 60)
    print("容器内增量更新脚本（仅下载变化的文件）")
    print("=" * 60)
    print(f"应用目录: {APP_DIR}")
    print(f"数据库路径: {DATABASE_PATH}")
    print(f"GitHub仓库: {GITHUB_REPO}")
    
    github_files = get_file_list_from_github()
    
    if not github_files:
        print("无法获取文件列表，退出")
        return
    
    files_to_update = check_files_to_update(github_files)
    
    steps = [
        ("下载更新文件", lambda: update_files(files_to_update)),
        ("更新数据库", update_database),
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
