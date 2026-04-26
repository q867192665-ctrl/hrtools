-- ========================================
-- 工资系统数据库建表脚本
-- 版本：v1.1
-- 创建日期：2026-04-07
-- 更新日期：2026-04-26
-- 说明：修复重复列定义问题
-- ========================================

-- ========================================
-- 1. 工资表 (salary_table)
-- ========================================

CREATE TABLE IF NOT EXISTS salary_table (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    序号 TEXT NOT NULL,
    部门 TEXT NOT NULL,
    姓名 TEXT NOT NULL UNIQUE,
    岗位 TEXT,
    入职日期 DATE NOT NULL,
    应出勤天数 DECIMAL(4,1),
    实际出勤天数 DECIMAL(4,1),
    上门服务小时 INTEGER,
    基本工资底薪 DECIMAL(10,2),
    基本工资绩效 DECIMAL(10,2),
    基本工资其它补贴 DECIMAL(10,2),
    基本工资合计 DECIMAL(10,2),
    岗位工资 DECIMAL(10,2),
    护理员绩效工资 DECIMAL(10,2),
    交通费 DECIMAL(10,2),
    手机费 DECIMAL(10,2),
    奖金 DECIMAL(10,2),
    高温费 DECIMAL(10,2),
    应发工资 DECIMAL(10,2),
    应扣款项养老 DECIMAL(10,2),
    应扣款项医疗 DECIMAL(10,2),
    应扣款项失业 DECIMAL(10,2),
    应扣款项公积金 DECIMAL(10,2),
    应扣款项应缴个税 DECIMAL(10,2),
    应扣款项缺勤扣款 DECIMAL(10,2),
    其它扣款 DECIMAL(10,2),
    住宿扣款 DECIMAL(10,2),
    水电扣款 DECIMAL(10,2),
    应扣款项社保 DECIMAL(10,2),
    应扣款项个税 DECIMAL(10,2),
    应扣款项其他 DECIMAL(10,2),
    应扣款项合计 DECIMAL(10,2),
    实发工资 DECIMAL(10,2),
    是否代扣社保 BOOLEAN,
    备注 TEXT,
    月份 TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_salary_name ON salary_table(姓名);
CREATE INDEX IF NOT EXISTS idx_salary_dept ON salary_table(部门);
CREATE INDEX IF NOT EXISTS idx_salary_date ON salary_table(入职日期);

-- ========================================
-- 2. 汇总表 (summary_table)
-- ========================================

CREATE TABLE IF NOT EXISTS summary_table (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    序号 TEXT,
    部门 TEXT,
    姓名 TEXT NOT NULL,
    岗位 TEXT,
    手机号 TEXT,
    入职日期 DATE,
    应出勤天数 DECIMAL(4,1),
    实际出勤天数 DECIMAL(4,1),
    上门服务小时 INTEGER,
    基本工资底薪 DECIMAL(10,2),
    基本工资绩效 DECIMAL(10,2),
    基本工资合计 DECIMAL(10,2),
    岗位工资 DECIMAL(10,2),
    护理员绩效工资 DECIMAL(10,2),
    应发工资 DECIMAL(10,2),
    应扣款项社保 DECIMAL(10,2),
    应扣款项公积金 DECIMAL(10,2),
    应扣款项个税 DECIMAL(10,2),
    应扣款项其他 DECIMAL(10,2),
    应扣款项合计 DECIMAL(10,2),
    实发工资 DECIMAL(10,2),
    是否代扣社保 BOOLEAN,
    查询状态 TEXT DEFAULT '未查询',
    查询次数 INTEGER DEFAULT 0,
    签收状态 TEXT DEFAULT '未签收',
    最新签收时间 TIMESTAMP,
    签收方式 TEXT,
    签名图片 TEXT,
    查询数据编号 TEXT,
    月份 TEXT,
    基本工资其它补贴 DECIMAL(10,2),
    交通费 DECIMAL(10,2),
    手机费 DECIMAL(10,2),
    奖金 DECIMAL(10,2),
    高温费 DECIMAL(10,2),
    应扣款项缺勤扣款 DECIMAL(10,2),
    应扣款项养老 DECIMAL(10,2),
    应扣款项医疗 DECIMAL(10,2),
    应扣款项失业 DECIMAL(10,2),
    应扣款项应缴个税 DECIMAL(10,2),
    其它扣款 DECIMAL(10,2),
    住宿扣款 DECIMAL(10,2),
    水电扣款 DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_summary_name ON summary_table(姓名);
CREATE INDEX IF NOT EXISTS idx_summary_dept ON summary_table(部门);

-- ========================================
-- 3. 用户表 (users)
-- ========================================

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    姓名 TEXT NOT NULL UNIQUE,
    密码 TEXT NOT NULL,
    手机号 TEXT,
    部门 TEXT,
    入职日期 DATE,
    role TEXT DEFAULT 'user',
    token TEXT,
    token_expire_time TIMESTAMP,
    last_login_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_name ON users(姓名);

-- ========================================
-- 4. 导出任务表 (export_tasks)
-- ========================================

CREATE TABLE IF NOT EXISTS export_tasks (
    task_id TEXT PRIMARY KEY,
    status TEXT DEFAULT 'pending',
    progress INTEGER DEFAULT 0,
    total INTEGER DEFAULT 0,
    message TEXT,
    file_path TEXT,
    filename TEXT,
    error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 5. 设备绑定表 (device_bindings)
-- ========================================

CREATE TABLE IF NOT EXISTS device_bindings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    device_id TEXT NOT NULL,
    device_info TEXT,
    bound_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    is_active INTEGER DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_device_username ON device_bindings(username);
CREATE INDEX IF NOT EXISTS idx_device_id ON device_bindings(device_id);

-- ========================================
-- 6. 登录尝试记录表 (login_attempts)
-- ========================================

CREATE TABLE IF NOT EXISTS login_attempts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    device_id TEXT,
    device_info TEXT,
    attempt_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status TEXT,
    matched_username TEXT,
    ip_address TEXT
);

CREATE INDEX IF NOT EXISTS idx_login_username ON login_attempts(username);
CREATE INDEX IF NOT EXISTS idx_login_time ON login_attempts(attempt_time);

-- ========================================
-- 7. 客户档案表 (customer_archive)
-- ========================================

CREATE TABLE IF NOT EXISTS customer_archive (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    档案编号 TEXT,
    联系人 TEXT,
    年龄 TEXT,
    性别 TEXT,
    出生日期 TEXT,
    街道 TEXT,
    级别 TEXT,
    身份证 TEXT,
    联系电话 TEXT,
    现住址 TEXT,
    自述身高 TEXT,
    自述体重 TEXT,
    紧急联系电话 TEXT,
    居住情况 TEXT,
    慢性疾病 TEXT,
    使用药物 TEXT,
    意识 TEXT,
    生命体征 TEXT,
    四肢活动情况 TEXT,
    现在有无压疮 TEXT,
    部位 TEXT,
    压疮危险度评估 TEXT,
    日常生活活动能力评估 TEXT,
    跌倒坠床风险评估 TEXT,
    生活自理能力 TEXT,
    客户类型 TEXT,
    护理计划起始时间 TEXT,
    待遇结束时间 TEXT,
    护理计划及要点 TEXT,
    备注 TEXT,
    上传批次 TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_customer_name ON customer_archive(联系人);

-- ========================================
-- 8. 请假记录表 (leave_records)
-- ========================================

CREATE TABLE IF NOT EXISTS leave_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    姓名 TEXT NOT NULL,
    请假类型 TEXT NOT NULL,
    开始日期 DATE NOT NULL,
    结束日期 DATE NOT NULL,
    请假天数 DECIMAL(4,1) NOT NULL,
    请假原因 TEXT,
    申请时间 TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_leave_name ON leave_records(姓名);

-- ========================================
-- 初始化管理员账号
-- ========================================

INSERT OR IGNORE INTO users (姓名, 密码, role) 
VALUES ('admin', '__INITIAL_PASSWORD__', 'admin');
