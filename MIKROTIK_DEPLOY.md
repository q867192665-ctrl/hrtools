# 工资签收系统 - MikroTik hAP ax2 Docker 部署指南

## 设备信息

- **设备**: MikroTik hAP ax2 (C52iG-5HaxD2HaxD)
- **架构**: ARM64 (aarch64) - IPQ8071A 处理器
- **内存**: 512MB RAM
- **存储**: 128MB NAND（建议使用 USB 外接存储）
- **系统**: RouterOS v7.x（支持 Docker 容器）

## 一、RouterOS 容器功能准备

### 1. 确保 RouterOS 版本 ≥ 7.x
```
/system/package/update/install
```

### 2. 启用容器功能
```
/system/device-mode/update container=yes
```

### 3. 创建虚拟以太网接口（容器网络）
```
/interface/veth/add name=veth1 address=172.17.0.2/24 gateway=172.17.0.1
/interface/bridge/port/add bridge=bridge interface=veth1
/ip/address/add address=172.17.0.1/24 interface=bridge
```

### 4. 配置 NAT（容器访问外网）
```
/ip/firewall/nat/add chain=srcnat src-address=172.17.0.0/24 out-interface=bridge action=masquerade
```

### 5. 配置 DNS
```
/ip/dns/set allow-remote-requests=yes
```

## 二、构建镜像（在开发机上操作）

### 前提条件
- Docker 已安装并支持 buildx
- 已启用 QEMU 模拟（交叉编译 arm64）

```bash
# 安装 QEMU 模拟器（用于交叉编译）
docker run --privileged --rm tonistiigi/binfmt --install all

# 执行构建脚本
chmod +x build_docker.sh
./build_docker.sh
```

构建完成后会生成 `salary-system-arm64.tar.gz` 文件。

## 三、部署到 MikroTik

### 方案 A：USB 外接存储（推荐，设备内置存储仅 128MB）

1. 格式化 USB 存储并挂载：
```
/disk/format-drive usb1 file-system=ext4
/wait
/disk/print
```

2. 设置容器存储路径到 USB：
```
/container/config/set ram-high=400M tmpdir=disk1/tmp registry-url=https://registry-1.docker.io
```

### 方案 B：使用内置存储

```
/container/config/set ram-high=400M
```

### 上传和加载镜像

1. **通过 SCP 上传镜像**：
```bash
scp salary-system-arm64.tar.gz admin@<路由器IP>:/salary-system-arm64.tar.gz
```

2. **在 RouterOS 中加载镜像**：
```
/container/add file=salary-system-arm64.tar.gz interface=veth1 root-dir=disk1/containers/salary hostname=salary-system logging=yes
```

3. **配置环境变量**：
```
/container/envlist/add name=salary-env key=TZ value=Asia/Shanghai
/container/envlist/add name=salary-env key=DATABASE_PATH value=/app/database/salary_system.db
/container/envlist/add name=salary-env key=SIGNATURE_DIR value=/app/backend/signatures
/container/set envlist=salary-env [find where hostname=salary-system]
```

4. **配置端口映射**：
```
/ip/firewall/nat/add chain=dstnat dst-port=5000 protocol=tcp action=dst-nat to-addresses=172.17.0.2 to-ports=5000
```

5. **启动容器**：
```
/container/start [find where hostname=salary-system]
```

### 管理命令

```routeros
# 查看容器列表
/container/print

# 查看容器日志
/container/log/print

# 停止容器
/container/stop [find where hostname=salary-system]

# 重启容器
/container/restart [find where hostname=salary-system]

# 删除容器
/container/remove [find where hostname=salary-system]
```

## 四、访问系统

- **Web 管理界面**: `http://<路由器IP>:5000/login`
- **API 地址**: `http://<路由器IPv6地址>:5000`
- **默认管理员**: admin / admin123

## 五、Android 客户端配置

在登录页的服务器地址栏输入路由器的 IPv6 地址即可。

## 六、数据持久化

容器使用 Docker Volume 持久化以下数据：
- `/app/database/` - 数据库文件
- `/app/backend/signatures/` - 签名图片
- `/app/backend/exports/` - 导出文件

在 MikroTik 上这些数据存储在 `root-dir` 指定的目录中，容器重启后数据不会丢失。

## 七、内存优化

hAP ax2 仅有 512MB RAM，已做以下优化：
- gunicorn worker 数量设为 **2**（默认4）
- 使用 slim 基础镜像减少内存占用
- 使用 SQLite 而非 MySQL/PostgreSQL

如遇内存不足，可减少 worker 数量为 1：
```routeros
/container/cmd [find where hostname=salary-system] cmd="gunicorn --bind [::]:5000 --workers 1 --timeout 120 --access-logfile - app:app"
```

## 八、故障排查

### 容器无法启动
```
/container/log/print
```

### 检查容器状态
```
/container/print status
```

### 网络不通
```
ping 172.17.0.2
/interface/veth/print
/ip/firewall/nat/print
```

### 端口无法访问
```
/ip/firewall/filter/print
# 确保 INPUT 链允许 TCP 5000 端口
/ip/firewall/filter/add chain=input protocol=tcp dst-port=5000 action=accept
```
