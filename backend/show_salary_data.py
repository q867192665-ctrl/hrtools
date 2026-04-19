#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sqlite3

conn = sqlite3.connect('e:/22/database/salary_system.db')
cursor = conn.cursor()

cursor.execute('SELECT * FROM salary_table')
rows = cursor.fetchall()

cursor.execute('PRAGMA table_info(salary_table)')
cols = [r[1] for r in cursor.fetchall()]

print('=' * 80)
print('当前数据库中的工资数据')
print('=' * 80)
print(f'共 {len(rows)} 条记录\n')

for row in rows:
    data = dict(zip(cols, row))
    print(f"【{data['姓名']}】")
    print(f"  序号: {data['序号']}")
    print(f"  部门: {data['部门']}")
    print(f"  岗位: {data['岗位']}")
    print(f"  入职日期: {data['入职日期']}")
    print(f"  出勤: 应出勤{data['应出勤天数']}天, 实际出勤{data['实际出勤天数']}天, 上门服务{data['上门服务小时']}小时")
    print(f"  基本工资: 底薪{data['基本工资底薪']}, 绩效{data['基本工资绩效']}, 合计{data['基本工资合计']}")
    print(f"  岗位工资: {data['岗位工资']}")
    print(f"  护理员绩效工资: {data['护理员绩效工资']}")
    print(f"  应发工资: {data['应发工资']}")
    print(f"  扣款: 社保{data['应扣款项社保']}, 公积金{data['应扣款项公积金']}, 个税{data['应扣款项个税']}, 其他{data['应扣款项其他']}, 合计{data['应扣款项合计']}")
    print(f"  实发工资: {data['实发工资']}")
    print(f"  是否代扣社保: {data['是否代扣社保']}")
    print(f"  备注: {data['备注']}")
    print('-' * 80)

conn.close()
