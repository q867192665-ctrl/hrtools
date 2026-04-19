#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""检查模板文件"""

import os

# 获取当前文件所在目录
current_dir = os.path.dirname(os.path.abspath(__file__))
template_path = os.path.join(current_dir, 'templates', 'admin.html')

print('=== 模板文件检查 ===')
print('模板文件路径:', template_path)
print('文件存在:', os.path.exists(template_path))

if os.path.exists(template_path):
    with open(template_path, 'r', encoding='utf-8') as f:
        content = f.read()
        print('文件大小:', len(content))
        print('包含月份输入框:', '工资表所属月份' in content)
        print('包含导出月份选择:', '选择月份' in content)
        print('包含导出Excel功能:', 'exportExcel' in content)
        
        # 检查upload标签页内容
        if '工资表所属月份' in content:
            print('\n✅ 模板文件包含月份选择功能')
        else:
            print('\n❌ 模板文件不包含月份选择功能')
            
            # 显示upload标签页内容的前500字符
            upload_start = content.find('<div id="upload"')
            if upload_start != -1:
                upload_end = content.find('</div>', upload_start) + 6
                print('Upload标签页内容（前500字符）:')
                print(content[upload_start:min(upload_end, upload_start+500)])
else:
    print('❌ 模板文件不存在')