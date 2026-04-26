# 修改 app.py
with open('/app/backend/app.py', 'r', encoding='utf-8') as f:
    content = f.read()

batch_api = '''        return jsonify({
            'success': False,
            'error': f'添加用户失败: {str(e)}'
        }), 500


@app.route('/api/admin/users/batch-add', methods=['POST'])
@admin_required
def batch_add_users():
    """
    批量添加用户（管理员）
    
    请求体：
    {
        "users": [
            {"name": "张三", "department": "技术部", "phone": "13800138000"},
            {"name": "李四", "department": "财务部", "phone": "13900139000"}
        ]
    }
    
    密码自动设置为手机号
    """
    try:
        data = request.get_json()
        users = data.get('users', [])
        
        if not users or not isinstance(users, list):
            return jsonify({
                'success': False,
                'error': '请提供用户列表'
            }), 400
        
        results = {
            'success_count': 0,
            'skip_count': 0,
            'error_count': 0,
            'details': []
        }
        
        conn = get_db_connection()
        cursor = conn.cursor()
        
        for user in users:
            name = user.get('name', '').strip()
            department = user.get('department', '').strip()
            phone = user.get('phone', '').strip()
            
            if not name or not department or not phone:
                results['skip_count'] += 1
                results['details'].append(f'{name or "未知"}: 信息不完整，跳过')
                continue
            
            cursor.execute("SELECT 姓名 FROM users WHERE 姓名 = ?", (name,))
            if cursor.fetchone():
                results['skip_count'] += 1
                results['details'].append(f'{name}: 用户已存在，跳过')
                continue
            
            from werkzeug.security import generate_password_hash
            hashed_password = generate_password_hash(phone, method='pbkdf2:sha256:10000')
            
            cursor.execute(
                "INSERT INTO users (姓名, 密码, 手机号, 部门, role) VALUES (?, ?, ?, ?, 'user')",
                (name, hashed_password, phone, department)
            )
            
            cursor.execute(
                """INSERT INTO summary_table (序号, 部门, 姓名, 入职日期, 手机号, 签收状态, 查询状态)
                VALUES (?, ?, ?, date('now'), ?, '未签收', '未查询')""",
                (f'NEW-{name[:4]}', department, name, phone)
            )
            
            results['success_count'] += 1
            results['details'].append(f'{name}: 添加成功')
        
        conn.commit()
        conn.close()
        
        message = f'批量导入完成！成功 {results["success_count"]} 人'
        if results['skip_count'] > 0:
            message += f'，跳过 {results["skip_count"]} 人'
        message += '\\n\\n详情：\\n' + '\\n'.join(results['details'])
        
        return jsonify({
            'success': True,
            'message': message,
            'data': results
        })
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': f'批量添加用户失败: {str(e)}'
        }), 500


@app.route('/api/admin/users/delete', methods=['POST'])'''

content = content.replace(
    '''        return jsonify({
            'success': False,
            'error': f'添加用户失败: {str(e)}'
        }), 500


@app.route('/api/admin/users/delete', methods=['POST'])''',
    batch_api
)

with open('/app/backend/app.py', 'w', encoding='utf-8') as f:
    f.write(content)

print('app.py 修改完成')
