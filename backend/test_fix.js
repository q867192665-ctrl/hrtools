// 检查服务器返回的HTML内容
fetch('/admin')
  .then(response => response.text())
  .then(html => {
    console.log('=== 服务器返回的HTML内容检查 ===');
    
    // 检查是否包含月份选择相关代码
    const hasMonthInput = html.includes('工资表所属月份');
    const hasExportMonth = html.includes('选择月份');
    const hasExportExcel = html.includes('exportExcel');
    
    console.log('包含月份输入框:', hasMonthInput);
    console.log('包含导出月份选择:', hasExportMonth);
    console.log('包含导出Excel功能:', hasExportExcel);
    
    // 如果包含功能，显示upload标签页内容
    if (hasMonthInput) {
      console.log('\\n=== Upload标签页内容（前500字符）===');
      const uploadMatch = html.match(/<div id="upload"[^>]*>([\\s\\S]*?)<\\/div>/);
      if (uploadMatch) {
        console.log(uploadMatch[1].substring(0, 500));
      }
    } else {
      console.log('\\n=== 当前Upload标签页内容 ===');
      console.log(document.getElementById('upload').innerHTML);
    }
  });