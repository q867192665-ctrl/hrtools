import sqlite3
import os
from datetime import datetime
from typing import Optional, Dict, List, Tuple

DATABASE_PATH = os.path.join(os.path.dirname(__file__), '..', 'database', 'salary_system.db')

class DeviceManager:
    def __init__(self, db_path: str = DATABASE_PATH):
        self.db_path = db_path
    
    def get_db_connection(self):
        conn = sqlite3.connect(self.db_path, timeout=60)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA journal_mode=WAL")
        conn.execute("PRAGMA busy_timeout=60000")
        conn.execute("PRAGMA wal_checkpoint(PASSIVE)")
        return conn
    
    def bind_device(self, username: str, device_id: str, device_info: str = '') -> Tuple[bool, str]:
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                "SELECT * FROM device_bindings WHERE username = ? AND is_active = 1",
                (username,)
            )
            existing_active = cursor.fetchone()
            
            if existing_active:
                if existing_active['device_id'] == device_id:
                    cursor.execute(
                        "UPDATE device_bindings SET last_login_at = ? WHERE id = ?",
                        (datetime.now(), existing_active['id'])
                    )
                    conn.commit()
                    return True, "设备已绑定，更新登录时间"
                else:
                    return False, "该账号已绑定其他设备"
            
            cursor.execute(
                "SELECT * FROM device_bindings WHERE device_id = ? AND is_active = 1",
                (device_id,)
            )
            device_bound = cursor.fetchone()
            
            if device_bound and device_bound['username'] != username:
                return False, f"该设备已绑定其他账号({device_bound['username']})"
            
            cursor.execute(
                "SELECT * FROM device_bindings WHERE username = ? AND device_id = ?",
                (username, device_id)
            )
            existing_inactive = cursor.fetchone()
            
            if existing_inactive:
                cursor.execute(
                    "UPDATE device_bindings SET is_active = 1, last_login_at = ?, device_info = ? WHERE id = ?",
                    (datetime.now(), device_info, existing_inactive['id'])
                )
                conn.commit()
                return True, "设备重新绑定成功"
            
            cursor.execute(
                """INSERT INTO device_bindings (username, device_id, device_info, bound_at, last_login_at, is_active)
                   VALUES (?, ?, ?, ?, ?, 1)""",
                (username, device_id, device_info, datetime.now(), datetime.now())
            )
            
            conn.commit()
            
            return True, "设备绑定成功"
            
        except Exception as e:
            print(f"[ERROR] 设备绑定失败: {e}")
            return False, f"绑定失败: {str(e)}"
        finally:
            if conn:
                conn.close()
    
    def verify_device(self, username: str, device_id: str) -> Tuple[bool, str, Optional[str]]:
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                "SELECT * FROM device_bindings WHERE username = ? AND is_active = 1",
                (username,)
            )
            binding = cursor.fetchone()
            
            if not binding:
                cursor.execute(
                    "SELECT username FROM device_bindings WHERE device_id = ? AND is_active = 1",
                    (device_id,)
                )
                device_owner = cursor.fetchone()
                if device_owner:
                    return False, f"该设备已绑定其他账号({device_owner['username']})", device_owner['username']
                return True, "首次登录，需要绑定设备", None
            
            if binding['device_id'] == device_id:
                cursor.execute(
                    "UPDATE device_bindings SET last_login_at = ? WHERE id = ?",
                    (datetime.now(), binding['id'])
                )
                conn.commit()
                return True, "设备验证通过", None
            
            cursor.execute(
                "SELECT username FROM device_bindings WHERE device_id = ? AND is_active = 1",
                (device_id,)
            )
            matched = cursor.fetchone()
            matched_username = matched['username'] if matched else None
            
            return False, f"设备ID与账号不符，该设备已绑定账号({matched_username})", matched_username
            
        except Exception as e:
            print(f"[ERROR] 设备验证失败: {e}")
            return False, f"验证失败: {str(e)}", None
        finally:
            if conn:
                conn.close()
    
    def record_login_attempt(self, username: str, device_id: str, device_info: str, 
                            status: str, matched_username: str = None, ip_address: str = None):
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                """INSERT INTO login_attempts 
                   (username, device_id, device_info, attempt_time, status, matched_username, ip_address)
                   VALUES (?, ?, ?, ?, ?, ?, ?)""",
                (username, device_id, device_info, datetime.now(), status, matched_username, ip_address)
            )
            
            conn.commit()
            
        except Exception as e:
            print(f"[ERROR] 记录登录尝试失败: {e}")
        finally:
            if conn:
                conn.close()
    
    def unbind_device(self, username: str) -> bool:
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                "UPDATE device_bindings SET is_active = 0 WHERE username = ?",
                (username,)
            )
            
            conn.commit()
            
            return True
            
        except Exception as e:
            print(f"[ERROR] 解绑设备失败: {e}")
            return False
        finally:
            if conn:
                conn.close()
    
    def get_all_bindings(self) -> List[Dict]:
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                """SELECT * FROM device_bindings WHERE is_active = 1 ORDER BY bound_at DESC"""
            )
            
            bindings = [dict(row) for row in cursor.fetchall()]
            
            return bindings
            
        except Exception as e:
            print(f"[ERROR] 获取设备绑定列表失败: {e}")
            return []
        finally:
            if conn:
                conn.close()
    
    def get_failed_attempts(self, limit: int = 50) -> List[Dict]:
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                """SELECT * FROM login_attempts 
                   WHERE status = 'failed' 
                   ORDER BY attempt_time DESC 
                   LIMIT ?""",
                (limit,)
            )
            
            attempts = [dict(row) for row in cursor.fetchall()]
            
            return attempts
            
        except Exception as e:
            print(f"[ERROR] 获取异常登录记录失败: {e}")
            return []
        finally:
            if conn:
                conn.close()
    
    def get_device_by_username(self, username: str) -> Optional[Dict]:
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                "SELECT * FROM device_bindings WHERE username = ? AND is_active = 1",
                (username,)
            )
            
            binding = cursor.fetchone()
            
            if binding:
                return dict(binding)
            return None
            
        except Exception as e:
            print(f"[ERROR] 获取设备绑定失败: {e}")
            return None
        finally:
            if conn:
                conn.close()
