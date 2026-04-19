#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
签名文件管理模块
功能：签名文件上传、存储、验证、查询
"""

import sqlite3
import os
import base64
import uuid
from datetime import datetime
from typing import Dict, List, Optional, Tuple

DATABASE_PATH = os.path.join(os.path.dirname(__file__), '..', 'database', 'salary_system.db')
SIGNATURE_DIR = os.path.join(os.path.dirname(__file__), 'signatures')

MAX_FILE_SIZE = 10 * 1024 * 1024  # 10MB
ALLOWED_FORMATS = ['JPG', 'PNG', 'PDF']


class SignatureManager:
    """签名文件管理器"""
    
    def __init__(self, db_path=None, signature_dir=None):
        self.db_path = db_path or DATABASE_PATH
        self.signature_dir = signature_dir or SIGNATURE_DIR
        self._ensure_signature_dir()
    
    def _ensure_signature_dir(self):
        """确保签名目录存在"""
        if not os.path.exists(self.signature_dir):
            os.makedirs(self.signature_dir)
            print(f"[INFO] 创建签名目录: {self.signature_dir}")
    
    def get_db_connection(self):
        """获取数据库连接"""
        conn = sqlite3.connect(self.db_path, timeout=60)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA journal_mode=WAL")
        conn.execute("PRAGMA busy_timeout=60000")
        conn.execute("PRAGMA wal_checkpoint(PASSIVE)")
        return conn
    
    def validate_file_format(self, file_format: str) -> bool:
        """
        验证文件格式
        
        Args:
            file_format: 文件格式（JPG/PNG/PDF）
        
        Returns:
            bool: 是否有效
        """
        return file_format.upper() in ALLOWED_FORMATS
    
    def validate_file_size(self, file_size: int) -> bool:
        """
        验证文件大小
        
        Args:
            file_size: 文件大小（字节）
        
        Returns:
            bool: 是否有效
        """
        return 0 < file_size <= MAX_FILE_SIZE
    
    def generate_file_id(self, user_name: str) -> str:
        """
        生成文件ID
        
        Args:
            user_name: 用户姓名
        
        Returns:
            str: 文件ID
        """
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        unique_id = uuid.uuid4().hex[:8]
        return f"{user_name}_{timestamp}_{unique_id}"
    
    def upload_signature(
        self, 
        user_name: str, 
        signature_data: str, 
        file_format: str = 'PNG'
    ) -> Dict:
        """
        上传签名文件
        
        Args:
            user_name: 用户姓名
            signature_data: Base64编码的签名数据
            file_format: 文件格式（JPG/PNG/PDF）
        
        Returns:
            dict: {
                'success': bool,
                'file_id': str (if success),
                'file_path': str (if success),
                'error': str (if failed)
            }
        """
        try:
            # 验证文件格式
            if not self.validate_file_format(file_format):
                return {
                    'success': False,
                    'error': f'不支持的文件格式: {file_format}。支持的格式: {", ".join(ALLOWED_FORMATS)}'
                }
            
            # 处理Base64数据
            if signature_data.startswith('data:image'):
                signature_data = signature_data.split(',')[1]
            
            # 解码Base64数据
            try:
                image_data = base64.b64decode(signature_data)
            except Exception as e:
                return {
                    'success': False,
                    'error': f'Base64解码失败: {str(e)}'
                }
            
            # 验证文件大小
            file_size = len(image_data)
            if not self.validate_file_size(file_size):
                return {
                    'success': False,
                    'error': f'文件大小超出限制: {file_size}字节。最大允许: {MAX_FILE_SIZE}字节（10MB）'
                }
            
            # 生成文件ID和文件名
            file_id = self.generate_file_id(user_name)
            file_name = f"{file_id}.{file_format.lower()}"
            file_path = os.path.join(self.signature_dir, file_name)
            
            # 保存文件
            with open(file_path, 'wb') as f:
                f.write(image_data)
            
            # 保存到数据库
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                """INSERT INTO signature_files 
                   (文件ID, 用户姓名, 文件名, 存储路径, 文件格式, 文件大小)
                   VALUES (?, ?, ?, ?, ?, ?)""",
                (file_id, user_name, file_name, file_path, file_format.upper(), file_size)
            )
            
            conn.commit()
            conn.close()
            
            print(f"[INFO] 签名文件上传成功: {file_id}")
            
            return {
                'success': True,
                'file_id': file_id,
                'file_path': file_path,
                'file_name': file_name,
                'file_size': file_size
            }
            
        except Exception as e:
            print(f"[ERROR] 上传签名文件失败: {e}")
            return {
                'success': False,
                'error': f'上传失败: {str(e)}'
            }
    
    def update_sign_status(self, user_name: str, file_id: str, month: str = '') -> bool:
        """
        更新签收状态
        
        Args:
            user_name: 用户姓名
            file_id: 签名文件ID
            month: 工资月份（可选）
        
        Returns:
            bool: 是否成功
        """
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                "SELECT 序号, 部门, 入职日期 FROM salary_table WHERE 姓名 = ? LIMIT 1",
                (user_name,)
            )
            user_info = cursor.fetchone()
            
            if user_info:
                serial_no = user_info['序号'] if user_info else 'SIG'
                department = user_info['部门'] if user_info else '待分配'
                hire_date = user_info['入职日期'] if user_info else '2024-01-01'
            else:
                cursor.execute(
                    "SELECT 部门 FROM users WHERE 姓名 = ?",
                    (user_name,)
                )
                user_row = cursor.fetchone()
                serial_no = 'SIG'
                department = user_row['部门'] if user_row else '待分配'
                hire_date = '2024-01-01'
            
            if month:
                cursor.execute(
                    "SELECT id FROM summary_table WHERE 姓名 = ? AND 月份 = ?",
                    (user_name, month)
                )
                
                existing = cursor.fetchone()
                
                if existing:
                    cursor.execute(
                        """UPDATE summary_table 
                           SET 签收状态 = '已签收',
                               最新签收时间 = ?,
                               签收方式 = '电子签名',
                               签名图片 = ?,
                               updated_at = ?
                           WHERE 姓名 = ? AND 月份 = ?""",
                        (datetime.now(), file_id, datetime.now(), user_name, month)
                    )
                else:
                    cursor.execute(
                        """INSERT INTO summary_table 
                           (序号, 部门, 姓名, 入职日期, 月份, 签收状态, 最新签收时间, 签收方式, 签名图片)
                           VALUES (?, ?, ?, ?, ?, '已签收', ?, '电子签名', ?)""",
                        (serial_no, department, user_name, hire_date, month, datetime.now(), file_id)
                    )
            else:
                cursor.execute(
                    "SELECT id FROM summary_table WHERE 姓名 = ?",
                    (user_name,)
                )
                
                existing = cursor.fetchone()
                
                if existing:
                    cursor.execute(
                        """UPDATE summary_table 
                           SET 签收状态 = '已签收',
                               最新签收时间 = ?,
                               签收方式 = '电子签名',
                               签名图片 = ?,
                               updated_at = ?
                           WHERE 姓名 = ?""",
                        (datetime.now(), file_id, datetime.now(), user_name)
                    )
                else:
                    cursor.execute(
                        """INSERT INTO summary_table 
                           (序号, 部门, 姓名, 入职日期, 签收状态, 最新签收时间, 签收方式, 签名图片)
                           VALUES (?, ?, ?, ?, '已签收', ?, '电子签名', ?)""",
                        (serial_no, department, user_name, hire_date, datetime.now(), file_id)
                    )
            
            conn.commit()
            conn.close()
            
            print(f"[INFO] 签收状态更新成功: {user_name}, 月份: {month if month else '全部'}")
            return True
            
        except Exception as e:
            print(f"[ERROR] 更新签收状态失败: {e}")
            return False
    
    def get_signature_file(self, file_id: str) -> Optional[Dict]:
        """
        获取签名文件信息
        
        Args:
            file_id: 文件ID
        
        Returns:
            dict: 文件信息，如果不存在返回None
        """
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                "SELECT * FROM signature_files WHERE 文件ID = ?",
                (file_id,)
            )
            
            file_info = cursor.fetchone()
            conn.close()
            
            if file_info:
                return dict(file_info)
            
            return None
            
        except Exception as e:
            print(f"[ERROR] 获取签名文件信息失败: {e}")
            return None
    
    def get_user_signatures(self, user_name: str) -> List[Dict]:
        """
        获取用户的所有签名文件
        
        Args:
            user_name: 用户姓名
        
        Returns:
            list: 签名文件列表
        """
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                """SELECT * FROM signature_files 
                   WHERE 用户姓名 = ? 
                   ORDER BY 上传时间 DESC""",
                (user_name,)
            )
            
            signatures = cursor.fetchall()
            conn.close()
            
            return [dict(sig) for sig in signatures]
            
        except Exception as e:
            print(f"[ERROR] 获取用户签名文件列表失败: {e}")
            return []
    
    def get_all_signatures(self) -> List[Dict]:
        """
        获取所有签名文件
        
        Returns:
            list: 所有签名文件列表
        """
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                "SELECT * FROM signature_files ORDER BY 上传时间 DESC"
            )
            
            signatures = cursor.fetchall()
            conn.close()
            
            return [dict(sig) for sig in signatures]
            
        except Exception as e:
            print(f"[ERROR] 获取所有签名文件列表失败: {e}")
            return []
    
    def delete_signature_file(self, file_id: str) -> bool:
        """
        删除签名文件
        
        Args:
            file_id: 文件ID
        
        Returns:
            bool: 是否成功
        """
        try:
            # 获取文件信息
            file_info = self.get_signature_file(file_id)
            
            if not file_info:
                print(f"[WARN] 签名文件不存在: {file_id}")
                return False
            
            # 删除物理文件
            file_path = file_info['存储路径']
            if os.path.exists(file_path):
                os.remove(file_path)
                print(f"[INFO] 删除物理文件: {file_path}")
            
            # 更新数据库状态
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                "UPDATE signature_files SET 文件状态 = '已删除' WHERE 文件ID = ?",
                (file_id,)
            )
            
            conn.commit()
            conn.close()
            
            print(f"[INFO] 签名文件删除成功: {file_id}")
            return True
            
        except Exception as e:
            print(f"[ERROR] 删除签名文件失败: {e}")
            return False
    
    def get_signature_statistics(self) -> Dict:
        """
        获取签名文件统计信息
        
        Returns:
            dict: 统计信息
        """
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            # 总签名文件数
            cursor.execute(
                "SELECT COUNT(*) FROM signature_files WHERE 文件状态 = '正常'"
            )
            total_files = cursor.fetchone()[0]
            
            # 按格式统计
            cursor.execute(
                """SELECT 文件格式, COUNT(*) as count 
                   FROM signature_files 
                   WHERE 文件状态 = '正常'
                   GROUP BY 文件格式"""
            )
            format_stats = cursor.fetchall()
            
            # 按用户统计
            cursor.execute(
                """SELECT 用户姓名, COUNT(*) as count 
                   FROM signature_files 
                   WHERE 文件状态 = '正常'
                   GROUP BY 用户姓名"""
            )
            user_stats = cursor.fetchall()
            
            # 总文件大小
            cursor.execute(
                "SELECT SUM(文件大小) FROM signature_files WHERE 文件状态 = '正常'"
            )
            total_size = cursor.fetchone()[0] or 0
            
            conn.close()
            
            return {
                'total_files': total_files,
                'total_size': total_size,
                'total_size_mb': round(total_size / (1024 * 1024), 2),
                'format_stats': [{'format': row['文件格式'], 'count': row['count']} for row in format_stats],
                'user_stats': [{'user': row['用户姓名'], 'count': row['count']} for row in user_stats]
            }
            
        except Exception as e:
            print(f"[ERROR] 获取签名文件统计信息失败: {e}")
            return {
                'total_files': 0,
                'total_size': 0,
                'total_size_mb': 0,
                'format_stats': [],
                'user_stats': []
            }
