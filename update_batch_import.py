#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批量导入人员功能 - 容器内更新脚本
运行方式: python3 /tmp/update_batch_import.py
"""

import os

# ===== 1. 更新 user_manage.html =====
html_path = '/app/backend/templates/user_manage.html'

with open(html_path, 'r', encoding='utf-8') as f:
    html = f.read()

# 1.1 添加批量导入按钮
old_btn = '<button class="btn btn-primary" onclick="showAddUserModal()">添加用户</button>\n                    <button class="btn btn-secondary" onclick="loadUserList()">刷新</button>'
new_btn = '<button class="btn btn-primary" onclick="showAddUserModal()">添加用户</button>\n                    <button class="btn btn-success" onclick="showBatchImportModal()">批量导入</button>\n                    <button class="btn btn-secondary" onclick="loadUserList()">刷新</button>'

if 'showBatchImportModal' not in html:
    html = html.replace(old_btn, new_btn)
    print('  ✓ 添加批量导入按钮')

# 1.2 添加批量导入模态框
modal_html = '''    </div>
    
    <div class="modal" id="batchImportModal">
        <div class="modal-content" style="width: 500px;">
            <div class="modal-header">
                <h3>批量导入人员</h3>
            </div>
            <div class="modal-body">
                <div class="form-group">
                    <label>选择 Excel 文件</label>
                    <input type="file" id="excelFileInput" accept=".xlsx,.xls" style="padding: 10px;">
                </div>
                <div style="background: var(--bg-tertiary); padding: 12px; border-radius: 4px; font-size: 13px; color: var(--text-secondary);">
                    <strong>📋 文件格式要求：</strong><br>
                    • B列：员工姓名（必填）<br>
                    • C列：部门（必填，如填写"离职"则跳过）<br>
                    • D列：手机号（必填，将作为登录密码）<br>
                    • 从第2行开始读取数据（第1行为表头）
                </div>
                <div id="importPreview" style="margin-top: 12px; display: none;">
                    <strong>预览：</strong>
                    <div id="previewContent" style="max-height: 200px; overflow-y: auto; margin-top: 8px; font-size: 13px;"></div>
                </div>
            </div>
            <div class="modal-footer">
                <button class="btn btn-secondary" onclick="closeBatchImportModal()">取消</button>
                <button class="btn btn-success" id="importBtn" onclick="batchImportUsers()" disabled>开始导入</button>
            </div>
        </div>
    </div>
    
    <div class="footer">'''

if 'batchImportModal' not in html:
    html = html.replace('    </div>\n    \n    <div class="footer">', modal_html)
    print('  ✓ 添加批量导入模态框')

# 1.3 添加 JavaScript 函数
js_code = '''        function goBack() {
            window.location.href = '/menu';
        }
        
        function showBatchImportModal() {
            document.getElementById('batchImportModal').style.display = 'flex';
            document.getElementById('excelFileInput').value = '';
            document.getElementById('importPreview').style.display = 'none';
            document.getElementById('importBtn').disabled = true;
        }
        
        function closeBatchImportModal() {
            document.getElementById('batchImportModal').style.display = 'none';
        }
        
        document.addEventListener('DOMContentLoaded', function() {
            document.getElementById('excelFileInput').addEventListener('change', handleFileSelect);
        });
        
        async function handleFileSelect(event) {
            const file = event.target.files[0];
            if (!file) return;
            
            const reader = new FileReader();
            reader.onload = async function(e) {
                try {
                    const arrayBuffer = e.target.result;
                    const data = new Uint8Array(arrayBuffer);
                    
                    const XLSX = await loadXLSX();
                    const workbook = XLSX.read(data, { type: 'array' });
                    const sheetName = workbook.SheetNames[0];
                    const worksheet = workbook.Sheets[sheetName];
                    const jsonData = XLSX.utils.sheet_to_json(worksheet, { header: 1 });
                    
                    const users = [];
                    const skipped = [];
                    
                    for (let i = 1; i < jsonData.length; i++) {
                        const row = jsonData[i];
                        if (!row || row.length < 4) continue;
                        
                        const name = String(row[1] || '').trim();
                        const department = String(row[2] || '').trim();
                        const phone = String(row[3] || '').trim();
                        
                        if (!name || !department || !phone) continue;
                        
                        if (department === '离职') {
                            skipped.push(name);
                            continue;
                        }
                        
                        users.push({ name, department, phone });
                    }
                    
                    const previewDiv = document.getElementById('importPreview');
                    const contentDiv = document.getElementById('previewContent');
                    
                    let html = '<div style="color: var(--success);">✅ 将导入 ' + users.length + ' 人</div>';
                    if (skipped.length > 0) {
                        html += '<div style="color: var(--warning); margin-top: 4px;">⏭️ 跳过 ' + skipped.length + ' 人（离职）：' + skipped.join('、') + '</div>';
                    }
                    html += '<table style="width: 100%; margin-top: 8px; font-size: 12px;"><tr><th>姓名</th><th>部门</th><th>手机号</th></tr>';
                    users.forEach(function(u) {
                        html += '<tr><td>' + u.name + '</td><td>' + u.department + '</td><td>' + u.phone + '</td></tr>';
                    });
                    html += '</table>';
                    
                    contentDiv.innerHTML = html;
                    previewDiv.style.display = 'block';
                    document.getElementById('importBtn').disabled = false;
                    
                    window._importUsers = users;
                    
                } catch (error) {
                    alert('解析 Excel 文件失败: ' + error.message);
                }
            };
            reader.readAsArrayBuffer(file);
        }
        
        async function loadXLSX() {
            if (window.XLSX) return window.XLSX;
            
            return new Promise(function(resolve, reject) {
                var script = document.createElement('script');
                script.src = 'https://cdn.jsdelivr.net/npm/xlsx@0.18.5/dist/xlsx.full.min.js';
                script.onload = function() { resolve(window.XLSX); };
                script.onerror = function() { reject(new Error('加载 XLSX 库失败')); };
                document.head.appendChild(script);
            });
        }
        
        async function batchImportUsers() {
            var users = window._importUsers || [];
            if (users.length === 0) {
                alert('没有可导入的用户数据');
                return;
            }
            
            var confirmMsg = '确定要导入 ' + users.length + ' 个用户吗？\\n密码将自动设置为手机号';
            if (!confirm(confirmMsg)) return;
            
            var importBtn = document.getElementById('importBtn');
            importBtn.disabled = true;
            importBtn.textContent = '导入中...';
            
            try {
                var response = await fetch('/api/admin/users/batch-add', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': token
                    },
                    body: JSON.stringify({ users: users })
                });
                var data = await response.json();
                
                if (data.success) {
                    alert(data.message);
                    closeBatchImportModal();
                    loadUserList();
                } else {
                    alert('导入失败: ' + data.error);
                }
            } catch (error) {
                alert('导入失败: ' + error.message);
            } finally {
                importBtn.disabled = false;
                importBtn.textContent = '开始导入';
            }
        }
        
        async function logout() {'''

if 'showBatchImportModal' not in html:
    html = html.replace(
        '''        function goBack() {
            window.location.href = '/menu';
        }
        
        async function logout() {''',
        js_code
    )
    print('  ✓ 添加 JavaScript 函数')

with open(html_path, 'w', encoding='utf-8') as f:
    f.write(html)

print('✓ user_manage.html 更新完成')

# ===== 2. 更新 app.py =====
app_path = '/app/backend/app.py'

with open(app_path, 'r', encoding='utf-8') as f:
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

if 'batch_add_users' not in content:
    content = content.replace(
        '''        return jsonify({
            'success': False,
            'error': f'添加用户失败: {str(e)}'
        }), 500


@app.route('/api/admin/users/delete', methods=['POST'])''',
        batch_api
    )
    
    with open(app_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print('✓ app.py 更新完成')
else:
    print('✓ app.py 已包含批量导入接口，跳过')

print('\n✅ 所有文件更新完成！')
print('请运行: docker restart hrtools 重启容器使更改生效')
