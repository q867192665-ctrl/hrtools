import sys

# 修改 user_manage.html
with open('/app/backend/templates/user_manage.html', 'r', encoding='utf-8') as f:
    html = f.read()

# 1. 添加批量导入按钮
html = html.replace(
    '<button class="btn btn-primary" onclick="showAddUserModal()">添加用户</button>\n                    <button class="btn btn-secondary" onclick="loadUserList()">刷新</button>',
    '<button class="btn btn-primary" onclick="showAddUserModal()">添加用户</button>\n                    <button class="btn btn-success" onclick="showBatchImportModal()">批量导入</button>\n                    <button class="btn btn-secondary" onclick="loadUserList()">刷新</button>'
)

# 2. 添加批量导入模态框
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

html = html.replace('    </div>\n    \n    <div class="footer">', modal_html)

# 3. 添加 JavaScript 函数
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
                    
                    let html = `<div style="color: var(--success);">✅ 将导入 ${users.length} 人</div>`;
                    if (skipped.length > 0) {
                        html += `<div style="color: var(--warning); margin-top: 4px;">⏭️ 跳过 ${skipped.length} 人（离职）：${skipped.join('、')}</div>`;
                    }
                    html += '<table style="width: 100%; margin-top: 8px; font-size: 12px;"><tr><th>姓名</th><th>部门</th><th>手机号</th></tr>';
                    users.forEach(u => {
                        html += `<tr><td>${u.name}</td><td>${u.department}</td><td>${u.phone}</td></tr>`;
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
            
            return new Promise((resolve, reject) => {
                const script = document.createElement('script');
                script.src = 'https://cdn.jsdelivr.net/npm/xlsx@0.18.5/dist/xlsx.full.min.js';
                script.onload = () => resolve(window.XLSX);
                script.onerror = () => reject(new Error('加载 XLSX 库失败'));
                document.head.appendChild(script);
            });
        }
        
        async function batchImportUsers() {
            const users = window._importUsers || [];
            if (users.length === 0) {
                alert('没有可导入的用户数据');
                return;
            }
            
            const confirmMsg = `确定要导入 ${users.length} 个用户吗？\\n密码将自动设置为手机号`;
            if (!confirm(confirmMsg)) return;
            
            const importBtn = document.getElementById('importBtn');
            importBtn.disabled = true;
            importBtn.textContent = '导入中...';
            
            try {
                const response = await fetch('/api/admin/users/batch-add', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': token
                    },
                    body: JSON.stringify({ users })
                });
                const data = await response.json();
                
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

html = html.replace(
    '''        function goBack() {
            window.location.href = '/menu';
        }
        
        async function logout() {''',
    js_code
)

with open('/app/backend/templates/user_manage.html', 'w', encoding='utf-8') as f:
    f.write(html)

print('user_manage.html 修改完成')
