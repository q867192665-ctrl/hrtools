# homeassistant 更新步骤

## 第一步：查找项目文件路径

在homeassistant上执行以下命令查找项目位置：

```bash
# 查找docker-compose.yml文件
find / -name "docker-compose.yml" 2>/dev/null

# 或者查找hrtools容器
docker inspect hrtools | grep -i "workingdir\|source"
```

## 第二步：上传更新文件

将以下文件上传到homeassistant的项目目录：
- `backend/app.py` （已修复的代码）
- `hr_update.sh` （更新脚本）

### 方法1：使用SCP从Windows上传

在Windows PowerShell中执行：
```powershell
# 替换 192.168.x.x 为homeassistant的实际IP
scp -r backend/ root@192.168.x.x:/实际项目路径/
scp hr_update.sh root@192.168.x.x:/实际项目路径/
scp docker-compose.yml root@192.168.x.x:/实际项目路径/
scp Dockerfile root@192.168.x.x:/实际项目路径/
```

### 方法2：使用SFTP工具

使用WinSCP、FileZilla等工具连接到homeassistant，上传文件。

## 第三步：执行更新

SSH登录到homeassistant后执行：

```bash
# 1. 进入项目目录（替换为实际路径）
cd /实际项目路径

# 2. 给脚本添加执行权限
chmod +x hr_update.sh

# 3. 编辑脚本，修改PROJECT_DIR为当前目录路径
vi hr_update.sh
# 修改第18行的 PROJECT_DIR="/实际项目路径"

# 4. 执行更新
./hr_update.sh
```

## 或者手动执行更新（不使用脚本）

```bash
# 1. 进入项目目录
cd /实际项目路径

# 2. 停止旧容器
docker stop hrtools
docker rm hrtools

# 3. 重新构建镜像
docker-compose build --no-cache

# 4. 启动新容器
docker-compose up -d

# 5. 查看日志
docker logs -f hrtools
```

## 快速查找项目路径的命令

```bash
# 方法1：查看hrtools容器的挂载路径
docker inspect hrtools --format='{{range .Mounts}}{{.Source}} -> {{.Destination}}{{println}}{{end}}'

# 方法2：查找包含app.py的目录
find / -name "app.py" -path "*/backend/*" 2>/dev/null

# 方法3：查找docker-compose.yml
find / -name "docker-compose.yml" 2>/dev/null
```
