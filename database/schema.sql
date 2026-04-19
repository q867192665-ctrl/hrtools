-- ========================================
-- 工资系统数据库建表脚本
-- 版本：v1.0
-- 创建日期：2026-04-07
-- 说明：严格遵循需求文档，确保字段1:1精准映射
-- ========================================

-- 创建数据库（如果使用SQLite，此步骤可省略）
-- CREATE DATABASE IF NOT EXISTS salary_system;

-- ========================================
-- 1. 工资表 (salary_table)
-- 用途：存储员工工资详细信息
-- 字段数：30个（含系统字段）
-- ========================================

CREATE TABLE IF NOT EXISTS salary_table (
    -- 主键
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    
    -- 基础信息类（文本类型）
    序号 TEXT NOT NULL,
    部门 TEXT NOT NULL,
    姓名 TEXT NOT NULL UNIQUE,
    岗位 TEXT,
    
    -- 日期类（日期类型，格式：YYYY-MM-DD）
    入职日期 DATE NOT NULL,
    
    -- 出勤类（数字类型，保留1位小数）
    应出勤天数 DECIMAL(4,1) CHECK (应出勤天数 >= 0),
    实际出勤天数 DECIMAL(4,1) CHECK (实际出勤天数 >= 0 AND 实际出勤天数 <= 应出勤天数),
    
    -- 上门服务小时（数字类型，保留0位小数）
    上门服务小时 INTEGER CHECK (上门服务小时 >= 0),
    
    -- 薪资类（数字类型，保留2位小数）
    基本工资底薪 DECIMAL(10,2) CHECK (基本工资底薪 >= 0),
    基本工资绩效 DECIMAL(10,2) CHECK (基本工资绩效 >= 0),
    基本工资其它补贴 DECIMAL(10,2) CHECK (基本工资其它补贴 >= 0),
    基本工资合计 DECIMAL(10,2) CHECK (基本工资合计 >= 0),
    岗位工资 DECIMAL(10,2) CHECK (岗位工资 >= 0),
    护理员绩效工资 DECIMAL(10,2) CHECK (护理员绩效工资 >= 0),
    交通费 DECIMAL(10,2) CHECK (交通费 >= 0),
    手机费 DECIMAL(10,2) CHECK (手机费 >= 0),
    奖金 DECIMAL(10,2) CHECK (奖金 >= 0),
    高温费 DECIMAL(10,2) CHECK (高温费 >= 0),
    应发工资 DECIMAL(10,2) CHECK (应发工资 >= 0),
    
    -- 扣款类（数字类型，保留2位小数）
    应扣款项养老 DECIMAL(10,2) CHECK (应扣款项养老 >= 0),
    应扣款项医疗 DECIMAL(10,2) CHECK (应扣款项医疗 >= 0),
    应扣款项失业 DECIMAL(10,2) CHECK (应扣款项失业 >= 0),
    应扣款项公积金 DECIMAL(10,2) CHECK (应扣款项公积金 >= 0),
    应扣款项应缴个税 DECIMAL(10,2) CHECK (应扣款项应缴个税 >= 0),
    应扣款项缺勤扣款 DECIMAL(10,2) CHECK (应扣款项缺勤扣款 >= 0),
    其它扣款 DECIMAL(10,2) CHECK (其它扣款 >= 0),
    住宿扣款 DECIMAL(10,2) CHECK (住宿扣款 >= 0),
    水电扣款 DECIMAL(10,2) CHECK (水电扣款 >= 0),
    应扣款项社保 DECIMAL(10,2) CHECK (应扣款项社保 >= 0),
    应扣款项公积金 DECIMAL(10,2) CHECK (应扣款项公积金 >= 0),
    应扣款项个税 DECIMAL(10,2) CHECK (应扣款项个税 >= 0),
    应扣款项其他 DECIMAL(10,2) CHECK (应扣款项其他 >= 0),
    应扣款项合计 DECIMAL(10,2) CHECK (应扣款项合计 >= 0),
    
    -- 实发工资（数字类型，保留2位小数）
    实发工资 DECIMAL(10,2) CHECK (实发工资 >= 0),
    
    -- 布尔类型
    是否代扣社保 BOOLEAN,
    
    -- 备注信息
    备注 TEXT,
    
    -- 月份
    月份 TEXT,
    
    -- 系统字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建工资表索引
CREATE INDEX IF NOT EXISTS idx_salary_name ON salary_table(姓名);
CREATE INDEX IF NOT EXISTS idx_salary_dept ON salary_table(部门);
CREATE INDEX IF NOT EXISTS idx_salary_date ON salary_table(入职日期);

-- ========================================
-- 2. 汇总表 (summary_table)
-- 用途：整合用户工资信息与签名状态
-- 字段数：29个（含系统字段）
-- 说明：与工资表字段1:1精准映射 + 状态字段
-- ========================================

CREATE TABLE IF NOT EXISTS summary_table (
    -- 主键
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    
    -- 基础信息类（与工资表1:1映射）
    序号 TEXT,
    部门 TEXT,
    姓名 TEXT NOT NULL,
    岗位 TEXT,
    手机号 TEXT,
    
    -- 日期类
    入职日期 DATE,
    
    -- 出勤类
    应出勤天数 DECIMAL(4,1),
    实际出勤天数 DECIMAL(4,1),
    上门服务小时 INTEGER,
    
    -- 薪资类（与工资表1:1映射）
    基本工资底薪 DECIMAL(10,2),
    基本工资绩效 DECIMAL(10,2),
    基本工资合计 DECIMAL(10,2),
    岗位工资 DECIMAL(10,2),
    护理员绩效工资 DECIMAL(10,2),
    应发工资 DECIMAL(10,2),
    
    -- 扣款类（与工资表1:1映射）
    应扣款项社保 DECIMAL(10,2),
    应扣款项公积金 DECIMAL(10,2),
    应扣款项个税 DECIMAL(10,2),
    应扣款项其他 DECIMAL(10,2),
    应扣款项合计 DECIMAL(10,2),
    
    -- 实发工资
    实发工资 DECIMAL(10,2),
    
    -- 布尔类型
    是否代扣社保 BOOLEAN,
    
    -- 状态字段（汇总表特有）
    查询状态 TEXT CHECK(查询状态 IN ('已查询', '未查询')) DEFAULT '未查询',
    查询次数 INTEGER DEFAULT 0 CHECK (查询次数 >= 0),
    签收状态 TEXT CHECK(签收状态 IN ('已签收', '未签收')) DEFAULT '未签收',
    最新签收时间 TIMESTAMP,
    签收方式 TEXT,
    签名图片 TEXT,
    查询数据编号 TEXT,
    
    -- 月份
    月份 TEXT,
    
    -- 额外工资项
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
    
    -- 系统字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 1
);

-- 创建汇总表索引
CREATE INDEX IF NOT EXISTS idx_summary_name ON summary_table(姓名);
CREATE INDEX IF NOT EXISTS idx_summary_dept ON summary_table(部门);
CREATE INDEX IF NOT EXISTS idx_summary_query_status ON summary_table(查询状态);
CREATE INDEX IF NOT EXISTS idx_summary_sign_status ON summary_table(签收状态);
CREATE INDEX IF NOT EXISTS idx_summary_version ON summary_table(version);

-- ========================================
-- 3. 用户表 (users)
-- 用途：用户身份认证与权限管理
-- 说明：基于RBAC权限模型
-- ========================================

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    姓名 TEXT NOT NULL UNIQUE,
    密码 TEXT NOT NULL,
    手机号 TEXT,
    部门 TEXT,
    入职日期 DATE,
    role TEXT CHECK(role IN ('user', 'admin')) DEFAULT 'user',
    token TEXT,
    token_expire_time TIMESTAMP,
    last_login_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建用户表索引
CREATE INDEX IF NOT EXISTS idx_users_name ON users(姓名);
CREATE INDEX IF NOT EXISTS idx_users_token ON users(token);

-- ========================================
-- 4. 签名文件表 (signature_files)
-- 用途：签名文件管理与索引
-- 说明：支持JPG/PNG/PDF格式
-- ========================================

CREATE TABLE IF NOT EXISTS signature_files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    文件ID TEXT NOT NULL UNIQUE,
    用户姓名 TEXT NOT NULL,
    文件名 TEXT NOT NULL,
    存储路径 TEXT NOT NULL,
    文件格式 TEXT CHECK(文件格式 IN ('JPG', 'PNG', 'PDF')),
    文件大小 INTEGER CHECK (文件大小 <= 10485760),
    上传时间 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    文件状态 TEXT CHECK(文件状态 IN ('正常', '损坏', '已删除')) DEFAULT '正常',
    FOREIGN KEY (用户姓名) REFERENCES users(姓名) ON DELETE CASCADE
);

-- 创建签名文件表索引
CREATE INDEX IF NOT EXISTS idx_signature_file_id ON signature_files(文件ID);
CREATE INDEX IF NOT EXISTS idx_signature_user ON signature_files(用户姓名);
CREATE INDEX IF NOT EXISTS idx_signature_status ON signature_files(文件状态);

-- ========================================
-- 5. 操作日志表 (operation_logs)
-- 用途：记录所有用户操作，支持审计追溯
-- 说明：日志保存时间≥6个月
-- ========================================

CREATE TABLE IF NOT EXISTS operation_logs (
    -- 主键
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    
    -- 操作信息
    操作人 TEXT NOT NULL,
    操作时间 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    操作类型 TEXT NOT NULL,
    操作内容 TEXT,
    操作结果 TEXT,
    IP地址 TEXT,
    
    -- 外键关联
    FOREIGN KEY (操作人) REFERENCES users(姓名) ON DELETE CASCADE
);

-- 创建操作日志表索引
CREATE INDEX IF NOT EXISTS idx_log_user ON operation_logs(操作人);
CREATE INDEX IF NOT EXISTS idx_log_time ON operation_logs(操作时间);
CREATE INDEX IF NOT EXISTS idx_log_type ON operation_logs(操作类型);

-- ========================================
-- 6. 数据库备份记录表 (backup_records)
-- 用途：记录数据库备份历史
-- 说明：支持备份恢复与版本管理
-- ========================================

CREATE TABLE IF NOT EXISTS backup_records (
    -- 主键
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    
    -- 备份信息
    备份时间 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    备份路径 TEXT NOT NULL,
    备份大小 INTEGER,
    备份类型 TEXT CHECK(备份类型 IN ('自动', '手动')),
    备份状态 TEXT CHECK(备份状态 IN ('成功', '失败')),
    备注 TEXT
);

-- ========================================
-- 7. 请假记录表 (leave_records)
-- 用途：员工请假申请记录
-- 说明：支持请假申请与审批流程
-- ========================================

CREATE TABLE IF NOT EXISTS leave_records (
    -- 主键
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    
    -- 请假信息
    姓名 TEXT NOT NULL,
    请假类型 TEXT NOT NULL,
    开始日期 DATE NOT NULL,
    结束日期 DATE NOT NULL,
    请假天数 DECIMAL(4,1) NOT NULL CHECK (请假天数 > 0),
    请假原因 TEXT,
    申请时间 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键关联
    FOREIGN KEY (姓名) REFERENCES users(姓名) ON DELETE CASCADE
);

-- 创建请假记录表索引
CREATE INDEX IF NOT EXISTS idx_leave_name ON leave_records(姓名);
CREATE INDEX IF NOT EXISTS idx_leave_date ON leave_records(开始日期);
CREATE INDEX IF NOT EXISTS idx_leave_type ON leave_records(请假类型);

-- ========================================
-- 8. 客户档案表 (customer_archive)
-- 用途：客户档案信息管理
-- 说明：支持Excel导入和纸质表导出
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
CREATE INDEX IF NOT EXISTS idx_customer_id ON customer_archive(档案编号);

-- ========================================
-- 9. 设备绑定表 (device_bindings)
-- 用途：用户设备绑定管理
-- 说明：一个账号只能绑定一个设备
-- ========================================

CREATE TABLE IF NOT EXISTS device_bindings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    device_id TEXT NOT NULL,
    device_info TEXT,
    bound_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    is_active INTEGER DEFAULT 1,
    FOREIGN KEY (username) REFERENCES users(姓名) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_device_username ON device_bindings(username);
CREATE INDEX IF NOT EXISTS idx_device_id ON device_bindings(device_id);

-- ========================================
-- 10. 登录尝试记录表 (login_attempts)
-- 用途：记录登录尝试，用于异常检测
-- 说明：包含成功和失败的登录记录
-- ========================================

CREATE TABLE IF NOT EXISTS login_attempts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    device_id TEXT,
    device_info TEXT,
    attempt_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status TEXT CHECK(status IN ('success', 'failed')),
    matched_username TEXT,
    ip_address TEXT
);

CREATE INDEX IF NOT EXISTS idx_login_username ON login_attempts(username);
CREATE INDEX IF NOT EXISTS idx_login_time ON login_attempts(attempt_time);
CREATE INDEX IF NOT EXISTS idx_login_status ON login_attempts(status);

-- ========================================
-- 初始化管理员账号
-- 说明：默认管理员账号，首次部署时创建
-- ========================================

INSERT OR IGNORE INTO users (姓名, 密码, role) 
VALUES ('admin', '__INITIAL_PASSWORD__', 'admin');

-- ========================================
-- 创建触发器：自动更新updated_at字段
-- ========================================

-- 工资表更新触发器
CREATE TRIGGER IF NOT EXISTS update_salary_timestamp 
AFTER UPDATE ON salary_table
FOR EACH ROW
BEGIN
    UPDATE salary_table SET updated_at = CURRENT_TIMESTAMP WHERE id = OLD.id;
END;

-- 汇总表更新触发器
CREATE TRIGGER IF NOT EXISTS update_summary_timestamp 
AFTER UPDATE ON summary_table
FOR EACH ROW
BEGIN
    UPDATE summary_table SET updated_at = CURRENT_TIMESTAMP WHERE id = OLD.id;
END;

-- ========================================
-- 创建视图：汇总表完整视图
-- 用途：方便查询汇总表完整信息
-- ========================================

CREATE VIEW IF NOT EXISTS v_summary_full AS
SELECT 
    s.id,
    s.序号,
    s.部门,
    s.姓名,
    s.入职日期,
    s.应出勤天数,
    s.实际出勤天数,
    s.上门服务小时,
    s.基本工资底薪,
    s.基本工资绩效,
    s.基本工资合计,
    s.岗位工资,
    s.护理员绩效工资,
    s.应发工资,
    s.应扣款项社保,
    s.应扣款项公积金,
    s.应扣款项个税,
    s.应扣款项其他,
    s.应扣款项合计,
    s.实发工资,
    s.是否代扣社保,
    s.查询状态,
    s.查询次数,
    s.签收状态,
    s.最新签收时间,
    s.签收方式,
    s.签名图片,
    s.查询数据编号,
    s.created_at,
    s.updated_at,
    s.version,
    sf.文件名 AS 签名文件名,
    sf.存储路径 AS 签名存储路径,
    sf.文件格式 AS 签名文件格式,
    sf.上传时间 AS 签名上传时间
FROM summary_table s
LEFT JOIN signature_files sf ON s.签名图片 = sf.文件ID;

-- ========================================
-- 数据完整性约束说明
-- ========================================

-- 1. 工资表与汇总表字段映射关系：
--    工资表.序号 → 汇总表.序号
--    工资表.部门 → 汇总表.部门
--    工资表.姓名 → 汇总表.姓名
--    工资表.入职日期 → 汇总表.入职日期
--    工资表.应出勤天数 → 汇总表.应出勤天数
--    工资表.实际出勤天数 → 汇总表.实际出勤天数
--    工资表.上门服务小时 → 汇总表.上门服务小时
--    工资表.基本工资底薪 → 汇总表.基本工资底薪
--    工资表.基本工资绩效 → 汇总表.基本工资绩效
--    工资表.基本工资合计 → 汇总表.基本工资合计
--    工资表.岗位工资 → 汇总表.岗位工资
--    工资表.护理员绩效工资 → 汇总表.护理员绩效工资
--    工资表.应发工资 → 汇总表.应发工资
--    工资表.应扣款项社保 → 汇总表.应扣款项社保
--    工资表.应扣款项公积金 → 汇总表.应扣款项公积金
--    工资表.应扣款项个税 → 汇总表.应扣款项个税
--    工资表.应扣款项其他 → 汇总表.应扣款项其他
--    工资表.应扣款项合计 → 汇总表.应扣款项合计
--    工资表.实发工资 → 汇总表.实发工资
--    工资表.是否代扣社保 → 汇总表.是否代扣社保

-- 2. 数据格式规范：
--    - 金额类：DECIMAL(10,2)，保留2位小数
--    - 出勤类：DECIMAL(4,1)，保留1位小数
--    - 上门服务小时：INTEGER，保留0位小数
--    - 日期类：DATE，格式YYYY-MM-DD
--    - 时间戳：TIMESTAMP，格式YYYY-MM-DD HH:MM:SS

-- 3. 数据约束：
--    - 金额类数据≥0
--    - 实际出勤天数≤应出勤天数
--    - 上门服务小时≥0
--    - 文件大小≤10M
--    - 查询次数≥0

-- ========================================
-- 脚本执行完成
-- ========================================
