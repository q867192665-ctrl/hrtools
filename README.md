# 工资签收系统

一个完整的工资签收管理系统，包含Android客户端和Flask后端服务。

## 系统功能

### Android客户端
- 用户登录界面
- 工资信息展示
- 手写签名功能
- 签名图片上传
- 状态实时更新

### 后端服务
- 用户认证管理
- 工资信息推送
- 签名状态管理
- 管理员功能
- Excel报表生成

## 系统架构

```
├── Android客户端 (app/)
│   ├── LoginActivity - 登录界面
│   ├── SalaryActivity - 工资签收界面
│   └── MainActivity - IPv6检测工具（保留）
├── 后端服务 (backend/)
│   ├── app.py - Flask API服务
│   ├── requirements.txt - Python依赖
│   └── run_backend.bat - 启动脚本
└── 数据库
    └── salary_system.db - SQLite数据库
```

## 安装和运行

### 后端服务

1. 确保已安装Python 3.7+
2. 进入backend目录：`cd backend`
3. 运行启动脚本：`run_backend.bat`
4. 服务将在 http://localhost:5000 启动

### Android应用

1. 使用Android Studio打开项目
2. 连接Android设备或启动模拟器
3. 构建并运行应用

## 默认账号

### 用户账号
- 用户名：`user1`，密码：`123456`
- 用户名：`user2`，密码：`123456`

### 管理员账号
- 用户名：`admin`，密码：`admin123`

## API接口

### 用户认证
- `POST /api/login` - 用户登录
- 请求体：`{"username": "user1", "password": "123456"}`

### 工资信息
- `GET /api/salary` - 获取工资信息（需要Authorization头）

### 签名功能
- `POST /api/signature` - 上传签名（需要Authorization头）
- 请求体：`{"signature": "base64图片数据"}`

### 管理员功能
- `GET /api/admin/status` - 获取所有用户状态
- `GET /api/admin/export` - 下载Excel报表
- `GET /api/admin/signatures` - 获取签名文件列表

## 数据库结构

### users表
- id: 用户ID
- username: 用户名
- password: 密码
- role: 角色（user/admin）

### salaries表
- id: 工资记录ID
- user_id: 用户ID
- salary_data: 工资信息JSON
- viewed: 是否已查看
- signed: 是否已签收
- signature_path: 签名图片路径

### login_logs表
- id: 登录记录ID
- user_id: 用户ID
- login_time: 登录时间

## 技术栈

### 前端
- Android (Kotlin)
- OkHttp3 (网络请求)
- Kotlin Coroutines (异步处理)

### 后端
- Flask (Python Web框架)
- SQLite (数据库)
- pandas (Excel处理)

## 注意事项

1. Android模拟器访问本地服务使用 `10.0.2.2:5000`
2. 实际设备需要确保与后端服务在同一网络
3. 签名图片保存在 `backend/signatures/` 目录
4. 数据库文件为 `backend/salary_system.db`

## 开发说明

- 系统支持中文字符路径（已配置android.overridePathCheck=true）
- 使用HTTP协议，生产环境建议使用HTTPS
- 密码为明文存储，生产环境应使用加密存储
- 签名功能支持手写签名并转换为base64上传