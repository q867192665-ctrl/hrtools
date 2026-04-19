#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
身份认证模块
功能：基于姓名的登录验证、Token管理、密码加密
"""

import sqlite3
import secrets
import hashlib
from datetime import datetime, timedelta
from functools import wraps
from flask import request, jsonify
from werkzeug.security import generate_password_hash, check_password_hash
import os

DATABASE_PATH = os.path.join(os.path.dirname(__file__), '..', 'database', 'salary_system.db')
TOKEN_EXPIRE_HOURS = 24
PASSWORD_HASH_METHOD = 'pbkdf2:sha256'
PASSWORD_HASH_ITERATIONS = 10000


class AuthManager:
    """身份认证管理器"""
    
    def __init__(self, db_path=None):
        self.db_path = db_path or DATABASE_PATH
    
    def get_db_connection(self):
        """获取数据库连接"""
        conn = sqlite3.connect(self.db_path, timeout=60)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA journal_mode=WAL")
        conn.execute("PRAGMA busy_timeout=60000")
        conn.execute("PRAGMA wal_checkpoint(TRUNCATE)")
        return conn
    
    def close_idle_connections(self):
        """关闭所有空闲连接"""
        try:
            conn = sqlite3.connect(self.db_path, timeout=10)
            conn.execute("PRAGMA wal_checkpoint(TRUNCATE)")
            conn.close()
        except:
            pass
    
    def generate_token(self):
        """生成安全的随机Token"""
        return secrets.token_urlsafe(32)
    
    def hash_token(self, token):
        """对Token进行哈希处理"""
        return hashlib.sha256(token.encode()).hexdigest()
    
    def verify_password(self, stored_password, provided_password):
        """验证密码"""
        if not stored_password:
            return False
        return check_password_hash(stored_password, provided_password)
    
    def login(self, name, password):
        """
        用户登录验证
        
        Args:
            name: 用户姓名
            password: 密码（明文）
        
        Returns:
            dict: {
                'success': bool,
                'token': str (if success),
                'role': str (if success),
                'name': str (if success),
                'error': str (if failed)
            }
        """
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                "SELECT 姓名, 密码, role FROM users WHERE 姓名 = ?",
                (name,)
            )
            user = cursor.fetchone()
            
            if not user:
                return {
                    'success': False,
                    'error': '用户不存在'
                }
            
            if not self.verify_password(user['密码'], password):
                return {
                    'success': False,
                    'error': '密码错误'
                }
            
            token = self.generate_token()
            token_hash = self.hash_token(token)
            expire_time = datetime.now() + timedelta(hours=TOKEN_EXPIRE_HOURS)
            
            cursor.execute(
                """UPDATE users 
                   SET token = ?, 
                       token_expire_time = ?, 
                       last_login_time = ? 
                   WHERE 姓名 = ?""",
                (token_hash, expire_time, datetime.now(), name)
            )
            
            conn.commit()
            
            return {
                'success': True,
                'token': token,
                'role': user['role'],
                'name': user['姓名']
            }
            
        except Exception as e:
            return {
                'success': False,
                'error': f'登录失败: {str(e)}'
            }
        finally:
            if conn:
                conn.close()
    
    def verify_token(self, token):
        """
        验证Token有效性
        
        Args:
            token: Token字符串
        
        Returns:
            dict: {
                'valid': bool,
                'name': str (if valid),
                'role': str (if valid),
                'error': str (if invalid)
            }
        """
        conn = None
        try:
            if not token:
                return {
                    'valid': False,
                    'error': 'Token为空'
                }
            
            token_hash = self.hash_token(token)
            
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                """SELECT 姓名, role, token_expire_time 
                   FROM users 
                   WHERE token = ?""",
                (token_hash,)
            )
            user = cursor.fetchone()
            
            if not user:
                return {
                    'valid': False,
                    'error': 'Token无效'
                }
            
            expire_time = datetime.fromisoformat(user['token_expire_time']) if user['token_expire_time'] else None
            
            if expire_time and expire_time < datetime.now():
                return {
                    'valid': False,
                    'error': 'Token已过期'
                }
            
            return {
                'valid': True,
                'name': user['姓名'],
                'role': user['role']
            }
            
        except Exception as e:
            return {
                'valid': False,
                'error': f'Token验证失败: {str(e)}'
            }
        finally:
            if conn:
                conn.close()
    
    def logout(self, name):
        """
        用户登出（清除Token）
        
        Args:
            name: 用户姓名
        
        Returns:
            bool: 是否成功
        """
        conn = None
        max_retries = 3
        for attempt in range(max_retries):
            try:
                conn = self.get_db_connection()
                cursor = conn.cursor()
                
                cursor.execute(
                    "UPDATE users SET token = NULL, token_expire_time = NULL WHERE 姓名 = ?",
                    (name,)
                )
                
                conn.commit()
                conn.close()
                return True
                
            except Exception as e:
                print(f"登出失败 (尝试 {attempt + 1}/{max_retries}): {e}")
                if conn:
                    try:
                        conn.close()
                    except:
                        pass
                if attempt < max_retries - 1:
                    import time
                    time.sleep(0.5)
        
        return False
    
    def change_password(self, name, old_password, new_password):
        """
        修改密码
        
        Args:
            name: 用户姓名
            old_password: 旧密码
            new_password: 新密码
        
        Returns:
            dict: {
                'success': bool,
                'error': str (if failed)
            }
        """
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                "SELECT 密码 FROM users WHERE 姓名 = ?",
                (name,)
            )
            user = cursor.fetchone()
            
            if not user:
                return {
                    'success': False,
                    'error': '用户不存在'
                }
            
            if not self.verify_password(user['密码'], old_password):
                return {
                    'success': False,
                    'error': '旧密码错误'
                }
            
            hashed_password = generate_password_hash(new_password, method=f'{PASSWORD_HASH_METHOD}:{PASSWORD_HASH_ITERATIONS}')
            cursor.execute(
                "UPDATE users SET 密码 = ? WHERE 姓名 = ?",
                (hashed_password, name)
            )
            
            conn.commit()
            
            return {
                'success': True
            }
            
        except Exception as e:
            return {
                'success': False,
                'error': f'修改密码失败: {str(e)}'
            }
        finally:
            if conn:
                conn.close()


def login_required(f):
    """
    登录验证装饰器
    用于保护需要登录才能访问的API端点
    """
    @wraps(f)
    def decorated_function(*args, **kwargs):
        token = request.headers.get('Authorization') or request.args.get('token')
        
        if not token:
            return jsonify({
                'success': False,
                'error': '未提供Token'
            }), 401
        
        auth_manager = AuthManager()
        result = auth_manager.verify_token(token)
        
        if not result['valid']:
            return jsonify({
                'success': False,
                'error': result['error']
            }), 401
        
        request.user_name = result['name']
        request.user_role = result['role']
        
        return f(*args, **kwargs)
    
    return decorated_function


def admin_required(f):
    """
    管理员权限验证装饰器
    用于保护需要管理员权限的API端点
    """
    @wraps(f)
    def decorated_function(*args, **kwargs):
        token = request.headers.get('Authorization') or request.args.get('token')
        
        if not token:
            return jsonify({
                'success': False,
                'error': '未提供Token'
            }), 401
        
        auth_manager = AuthManager()
        result = auth_manager.verify_token(token)
        
        if not result['valid']:
            return jsonify({
                'success': False,
                'error': result['error']
            }), 401
        
        if result['role'] != 'admin':
            return jsonify({
                'success': False,
                'error': '权限不足'
            }), 403
        
        request.user_name = result['name']
        request.user_role = result['role']
        
        return f(*args, **kwargs)
    
    return decorated_function
