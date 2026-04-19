    def sync_all_to_summary(self) -> Dict:
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute("SELECT DISTINCT 姓名, 月份 FROM salary_table WHERE 月份 != ''")
            salary_records = cursor.fetchall()
            
            synced_count = 0
            error_count = 0
            errors = []
            
            for record in salary_records:
                name = record['姓名']
                month = record['月份'] or ''
                
                try:
                    cursor.execute(
                        "SELECT 手机号, 部门, 入职日期 FROM users WHERE 姓名 = ?",
                        (name,)
                    )
                    user_info = cursor.fetchone()
                    phone_number = user_info['手机号'] if user_info and user_info['手机号'] else None
                    user_dept = user_info['部门'] if user_info and user_info['部门'] else None
                    user_hire_date = user_info['入职日期'] if user_info and user_info['入职日期'] else None
                    
                    cursor.execute("SELECT * FROM salary_table WHERE 姓名 = ? AND 月份 = ?", (name, month))
                    salary_data = cursor.fetchone()
                    
                    if not salary_data:
                        errors.append(f"用户 {name} ({month}) 的工资数据不存在")
                        error_count += 1
                        continue
                    
                    cursor.execute(
                        "SELECT id FROM summary_table WHERE 姓名 = ? AND 月份 = ?",
                        (name, month)
                    )
                    existing = cursor.fetchone()
                    
                    if existing:
                        cursor.execute(
                            """UPDATE summary_table 
                               SET 序号 = ?,
                                   部门 = COALESCE(?, 部门),
                                   入职日期 = COALESCE(?, 入职日期),
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
                                   护理员绩效工资 = ?,
                                   应发工资 = ?,
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
                                   月份 = ?,
                                   updated_at = ?
                               WHERE 姓名 = ? AND 月份 = ?""",
                            (
                                salary_data.get('序号'),
                                user_dept or salary_data.get('部门'),
                                user_hire_date or salary_data.get('入职日期'),
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
                                salary_data.get('护理员绩效工资'),
                                salary_data.get('应发工资'),
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
                                month,
                                datetime.now(),
                                name, month
                            )
                        )
                    else:
                        cursor.execute(
                            """INSERT INTO summary_table 
                               (序号, 部门, 姓名, 入职日期, 手机号, 是否代扣社保, 岗位, 
                                应出勤天数, 实际出勤天数, 上门服务小时,
                                基本工资底薪, 基本工资其它补贴, 基本工资合计,
                                岗位工资, 交通费, 手机费, 奖金, 高温费,
                                护理员绩效工资, 应发工资,
                                应扣款项缺勤扣款, 应扣款项养老, 应扣款项医疗, 
                                应扣款项失业, 应扣款项公积金, 应扣款项应缴个税,
                                其它扣款, 住宿扣款, 水电扣款,
                                实发工资, 月份, 查询状态, 查询次数)
                               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '已查询', 1)""",
                            (
                                salary_data.get('序号'),
                                user_dept or salary_data.get('部门'),
                                name,
                                user_hire_date or salary_data.get('入职日期'),
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
                                salary_data.get('护理员绩效工资'),
                                salary_data.get('应发工资'),
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
                                month
                            )
                        )
                    
                    synced_count += 1
                    
                except Exception as e:
                    errors.append(f"同步用户 {name} ({month}) 失败: {str(e)}")
                    error_count += 1
            
            conn.commit()
            conn.close()
            
            print(f"[INFO] 数据汇总完成: 成功 {synced_count} 条, 失败 {error_count} 条")
            
            return {
                'success': True,
                'synced_count': synced_count,
                'error_count': error_count,
                'errors': errors
            }
            
        except Exception as e:
            print(f"[ERROR] 数据汇总失败: {e}")
            return {
                'success': False,
                'synced_count': 0,
                'error_count': 0,
                'errors': [str(e)]
            }