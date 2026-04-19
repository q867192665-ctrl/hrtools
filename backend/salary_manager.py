#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
工资数据管理模块
功能：工资数据查询、推送、状态更新
"""

import sqlite3
import os
from datetime import datetime
from typing import Dict, List, Optional, Tuple

DATABASE_PATH = os.path.join(os.path.dirname(__file__), '..', 'database', 'salary_system.db')


class SalaryManager:
    """工资数据管理器"""
    
    def __init__(self, db_path=None):
        self.db_path = db_path or DATABASE_PATH
    
    def get_db_connection(self):
        """获取数据库连接"""
        conn = sqlite3.connect(self.db_path, timeout=60)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA journal_mode=WAL")
        conn.execute("PRAGMA busy_timeout=60000")
        conn.execute("PRAGMA wal_checkpoint(PASSIVE)")
        return conn
    
    def get_salary_by_name(self, name: str) -> Optional[Dict]:
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                """SELECT * FROM salary_table 
                   WHERE 姓名 = ? 
                   ORDER BY created_at DESC 
                   LIMIT 1""",
                (name,)
            )
            
            salary = cursor.fetchone()
            
            if salary:
                return dict(salary)
            
            return None
            
        except Exception as e:
            print(f"[ERROR] 查询工资数据失败: {e}")
            return None
        finally:
            if conn:
                conn.close()
    
    def get_salary_by_name_and_month(self, name: str, month: str) -> Optional[Dict]:
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                """SELECT * FROM salary_table 
                   WHERE 姓名 = ? AND 月份 = ?
                   ORDER BY created_at DESC 
                   LIMIT 1""",
                (name, month)
            )
            
            salary = cursor.fetchone()
            
            if salary:
                return dict(salary)
            
            return None
            
        except Exception as e:
            print(f"[ERROR] 查询工资数据失败: {e}")
            return None
        finally:
            if conn:
                conn.close()
    
    def get_available_months(self, name: str = None) -> list:
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            if name:
                cursor.execute(
                    "SELECT DISTINCT 月份 FROM salary_table WHERE 姓名 = ? AND 月份 IS NOT NULL AND 月份 != '' ORDER BY 月份 DESC",
                    (name,)
                )
            else:
                cursor.execute(
                    "SELECT DISTINCT 月份 FROM salary_table WHERE 月份 IS NOT NULL AND 月份 != '' ORDER BY 月份 DESC"
                )
            
            months = [row['月份'] for row in cursor.fetchall()]
            
            return months
            
        except Exception as e:
            print(f"[ERROR] 查询月份列表失败: {e}")
            return []
        finally:
            if conn:
                conn.close()
    
    def sync_to_summary(self, name: str, month: str = '') -> bool:
        conn = None
        try:
            if month:
                salary_data = self.get_salary_by_name_and_month(name, month)
            else:
                salary_data = self.get_salary_by_name(name)
            
            if not salary_data:
                print(f"[WARN] 未找到用户 {name} 的工资数据")
                return False
            
            salary_month = month or salary_data.get('月份', '')
            
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                "SELECT 手机号 FROM users WHERE 姓名 = ?",
                (name,)
            )
            user_info = cursor.fetchone()
            phone_number = user_info['手机号'] if user_info else None
            
            cursor.execute(
                "SELECT id FROM summary_table WHERE 姓名 = ? AND 月份 = ?",
                (name, salary_month)
            )
            
            existing = cursor.fetchone()
            
            if existing:
                cursor.execute(
                    """UPDATE summary_table 
                       SET 序号 = ?,
                           部门 = ?,
                           入职日期 = ?,
                           手机号 = COALESCE(?, 手机号),
                           是否代扣社保 = ?,
                           岗位 = ?,
                           应出勤天数 = ?,
                           实际出勤天数 = ?,
                           上门服务小时 = ?,
                           基本工资底薪 = ?,
                           基本工资其它补贴 = ?,
                           基本工资合计 = ?,
                           岗位工资 = ?,
                           交通费 = ?,
                           手机费 = ?,
                           奖金 = ?,
                           高温费 = ?,
                           应扣款项缺勤扣款 = ?,
                           应扣款项养老 = ?,
                           应扣款项医疗 = ?,
                           应扣款项失业 = ?,
                           应扣款项公积金 = ?,
                           应扣款项应缴个税 = ?,
                           其它扣款 = ?,
                           住宿扣款 = ?,
                           水电扣款 = ?,
                           实发工资 = ?,
                           护理员绩效工资 = ?,
                           应发工资 = ?,
                           查询状态 = '已查询',
                           查询次数 = 查询次数 + 1,
                           updated_at = ?
                       WHERE 姓名 = ? AND 月份 = ?""",
                    (
                        salary_data.get('序号'),
                        salary_data.get('部门'),
                        salary_data.get('入职日期'),
                        phone_number,
                        salary_data.get('是否代扣社保'),
                        salary_data.get('岗位'),
                        salary_data.get('应出勤天数'),
                        salary_data.get('实际出勤天数'),
                        salary_data.get('上门服务小时'),
                        salary_data.get('基本工资底薪'),
                        salary_data.get('基本工资其它补贴'),
                        salary_data.get('基本工资合计'),
                        salary_data.get('岗位工资'),
                        salary_data.get('交通费'),
                        salary_data.get('手机费'),
                        salary_data.get('奖金'),
                        salary_data.get('高温费'),
                        salary_data.get('应扣款项缺勤扣款'),
                        salary_data.get('应扣款项养老'),
                        salary_data.get('应扣款项医疗'),
                        salary_data.get('应扣款项失业'),
                        salary_data.get('应扣款项公积金'),
                        salary_data.get('应扣款项应缴个税'),
                        salary_data.get('其它扣款'),
                        salary_data.get('住宿扣款'),
                        salary_data.get('水电扣款'),
                        salary_data.get('实发工资'),
                        salary_data.get('护理员绩效工资'),
                        salary_data.get('应发工资'),
                        datetime.now(),
                        name,
                        salary_month
                    )
                )
            else:
                cursor.execute(
                    """INSERT INTO summary_table 
                       (序号, 部门, 姓名, 入职日期, 手机号, 是否代扣社保, 岗位, 
                        应出勤天数, 实际出勤天数, 上门服务小时,
                        基本工资底薪, 基本工资其它补贴, 基本工资合计,
                        岗位工资, 交通费, 手机费, 奖金, 高温费,
                        应扣款项缺勤扣款, 应扣款项养老, 应扣款项医疗, 
                        应扣款项失业, 应扣款项公积金, 应扣款项应缴个税,
                        其它扣款, 住宿扣款, 水电扣款,
                        实发工资, 护理员绩效工资, 应发工资, 
                        月份, 查询状态, 查询次数)
                       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '已查询', 1)""",
                    (
                        salary_data.get('序号'),
                        salary_data.get('部门'),
                        name,
                        salary_data.get('入职日期'),
                        phone_number,
                        salary_data.get('是否代扣社保'),
                        salary_data.get('岗位'),
                        salary_data.get('应出勤天数'),
                        salary_data.get('实际出勤天数'),
                        salary_data.get('上门服务小时'),
                        salary_data.get('基本工资底薪'),
                        salary_data.get('基本工资其它补贴'),
                        salary_data.get('基本工资合计'),
                        salary_data.get('岗位工资'),
                        salary_data.get('交通费'),
                        salary_data.get('手机费'),
                        salary_data.get('奖金'),
                        salary_data.get('高温费'),
                        salary_data.get('应扣款项缺勤扣款'),
                        salary_data.get('应扣款项养老'),
                        salary_data.get('应扣款项医疗'),
                        salary_data.get('应扣款项失业'),
                        salary_data.get('应扣款项公积金'),
                        salary_data.get('应扣款项应缴个税'),
                        salary_data.get('其它扣款'),
                        salary_data.get('住宿扣款'),
                        salary_data.get('水电扣款'),
                        salary_data.get('实发工资'),
                        salary_data.get('护理员绩效工资'),
                        salary_data.get('应发工资'),
                        salary_month
                    )
                )
            
            conn.commit()
            
            return True
            
        except Exception as e:
            print(f"[ERROR] 同步工资数据到汇总表失败: {e}")
            return False
        finally:
            if conn:
                conn.close()
    
    def get_summary_by_name(self, name: str) -> Optional[Dict]:
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                """SELECT * FROM summary_table 
                   WHERE 姓名 = ? 
                   ORDER BY updated_at DESC 
                   LIMIT 1""",
                (name,)
            )
            
            summary = cursor.fetchone()
            
            if summary:
                return dict(summary)
            
            return None
            
        except Exception as e:
            print(f"[ERROR] 查询汇总数据失败: {e}")
            return None
        finally:
            if conn:
                conn.close()
    
    def get_summary_by_name_and_month(self, name: str, month: str) -> Optional[Dict]:
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                """SELECT * FROM summary_table 
                   WHERE 姓名 = ? AND 月份 = ?
                   ORDER BY updated_at DESC 
                   LIMIT 1""",
                (name, month)
            )
            
            summary = cursor.fetchone()
            
            if summary:
                return dict(summary)
            
            return None
            
        except Exception as e:
            print(f"[ERROR] 查询汇总数据失败: {e}")
            return None
        finally:
            if conn:
                conn.close()
    
    def get_all_summaries(self, department: Optional[str] = None, month: Optional[str] = None) -> List[Dict]:
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            conditions = ["st.姓名 = u.姓名"]
            params = []
            
            if department:
                conditions.append("st.部门 = ?")
                params.append(department)
            
            if month:
                conditions.append("st.月份 = ?")
                params.append(month)
            
            where_clause = " WHERE " + " AND ".join(conditions)
            
            cursor.execute(
                f"""SELECT st.* FROM summary_table st 
                    INNER JOIN users u ON st.姓名 = u.姓名 
                    {where_clause} 
                    ORDER BY CAST(st.序号 AS INTEGER)""",
                params
            )
            
            summaries = cursor.fetchall()
            
            return [dict(summary) for summary in summaries]
            
        except Exception as e:
            print(f"[ERROR] 查询所有汇总数据失败: {e}")
            return []
        finally:
            if conn:
                conn.close()
    
    def update_query_status(self, name: str, status: str = '已查询') -> bool:
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                """UPDATE summary_table 
                   SET 查询状态 = ?, 
                       updated_at = ?
                   WHERE 姓名 = ?""",
                (status, datetime.now(), name)
            )
            
            conn.commit()
            
            return True
            
        except Exception as e:
            print(f"[ERROR] 更新查询状态失败: {e}")
            return False
        finally:
            if conn:
                conn.close()
    
    def get_query_statistics(self, month: str = None) -> Dict:
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            if month:
                cursor.execute(
                    """SELECT COUNT(*) 
                       FROM users 
                       WHERE role = 'user'"""
                )
                total_users = cursor.fetchone()[0]
                
                cursor.execute(
                    """SELECT COUNT(DISTINCT u.姓名) 
                       FROM users u
                       LEFT JOIN salary_table s ON u.姓名 = s.姓名 AND s.月份 = ?
                       WHERE u.role = 'user' AND s.id IS NOT NULL""",
                    (month,)
                )
                salary_users = cursor.fetchone()[0]
                
                cursor.execute(
                    """SELECT COUNT(DISTINCT st.姓名) 
                       FROM summary_table st 
                       INNER JOIN users u ON st.姓名 = u.姓名 
                       WHERE st.签收状态 = '已签收' AND st.月份 = ?""",
                    (month,)
                )
                signed_users = cursor.fetchone()[0]
                
                cursor.execute(
                    """SELECT SUM(st.查询次数) 
                       FROM summary_table st 
                       INNER JOIN users u ON st.姓名 = u.姓名 
                       WHERE st.月份 = ?""",
                    (month,)
                )
                total_queries = cursor.fetchone()[0] or 0
                
                return {
                    'total_users': salary_users,
                    'queried_users': 0,
                    'signed_users': signed_users,
                    'total_queries': total_queries,
                    'query_rate': round(signed_users / salary_users * 100, 2) if salary_users > 0 else 0,
                    'sign_rate': round(signed_users / salary_users * 100, 2) if salary_users > 0 else 0
                }
            else:
                cursor.execute("SELECT COUNT(*) FROM users WHERE role = 'user'")
                total_users = cursor.fetchone()[0]
                
                cursor.execute(
                    """SELECT COUNT(*) 
                       FROM summary_table st 
                       INNER JOIN users u ON st.姓名 = u.姓名 
                       WHERE 查询状态 = '已查询'"""
                )
                queried_users = cursor.fetchone()[0]
                
                cursor.execute(
                    """SELECT COUNT(*) 
                       FROM summary_table st 
                       INNER JOIN users u ON st.姓名 = u.姓名 
                       WHERE 签收状态 = '已签收'"""
                )
                signed_users = cursor.fetchone()[0]
                
                cursor.execute(
                    """SELECT SUM(查询次数) 
                       FROM summary_table st 
                       INNER JOIN users u ON st.姓名 = u.姓名"""
                )
                total_queries = cursor.fetchone()[0] or 0
            
            return {
                'total_users': total_users,
                'queried_users': queried_users,
                'signed_users': signed_users,
                'total_queries': total_queries,
                'query_rate': round(queried_users / total_users * 100, 2) if total_users > 0 else 0,
                'sign_rate': round(signed_users / total_users * 100, 2) if total_users > 0 else 0
            }
            
        except Exception as e:
            print(f"[ERROR] 获取查询统计信息失败: {e}")
            return {
                'total_users': 0,
                'queried_users': 0,
                'signed_users': 0,
                'total_queries': 0,
                'query_rate': 0,
                'sign_rate': 0
            }
        finally:
            if conn:
                conn.close()
    
    def get_department_statistics(self) -> List[Dict]:
        conn = None
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                """SELECT 
                    部门,
                    COUNT(*) as total_count,
                    SUM(CASE WHEN 查询状态 = '已查询' THEN 1 ELSE 0 END) as queried_count,
                    SUM(CASE WHEN 签收状态 = '已签收' THEN 1 ELSE 0 END) as signed_count,
                    SUM(查询次数) as total_queries,
                    AVG(实发工资) as avg_salary
                   FROM summary_table
                   GROUP BY 部门
                   ORDER BY 部门"""
            )
            
            departments = cursor.fetchall()
            
            result = []
            for dept in departments:
                result.append({
                    'department': dept['部门'],
                    'total_count': dept['total_count'],
                    'queried_count': dept['queried_count'],
                    'signed_count': dept['signed_count'],
                    'total_queries': dept['total_queries'] or 0,
                    'avg_salary': round(dept['avg_salary'], 2) if dept['avg_salary'] else 0
                })
            
            return result
            
        except Exception as e:
            print(f"[ERROR] 获取部门统计信息失败: {e}")
            return []
        finally:
            if conn:
                conn.close()
