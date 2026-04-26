# homeassistant 更新完整步骤

## 第一步：查找项目源码位置

在homeassistant上执行以下命令：

```bash
# 1. 查看hrtools容器的完整挂载信息
docker inspect hrtools --format='{{json .Mounts}}' | python3 -m json.tool

# 2. 查找包含app.py的目录
find / -name "app.py" -path "*/backend/*" 2>/dev/null | grep -v overlay

# 3. 查找Dockerfile
find / -name "Dockerfile" 2>/dev/null | grep -v overlay

# 4. 查看docker-compose.yml的详细信息
docker inspect hrtools --format='{{json .Config}}' | python3 -m json.tool
```

## 第二步：上传更新文件

根据第一步找到的项目路径，将修改后的文件上传到该目录。

### 方法1：使用SCP（从Windows上传）

在Windows PowerShell中执行（替换IP地址和路径）：
```powershell
# 假设找到的项目路径是 /opt/hrtools
scp backend/app.py root@homeassistant的IP:/opt/hrtools/backend/
scp Dockerfile root@homeassistant的IP:/opt/hrtools/
scp docker-compose.yml root@homeassistant的IP:/opt/hrtools/
```

### 方法2：直接在容器内修改（临时方案）

```bash
# 进入容器
docker exec -it hrtools /bin/bash

# 查看当前app.py位置
ls -la /app/backend/app.py

# 退出容器
exit
```

## 第三步：执行更新

```bash
# 进入项目目录（替换为实际路径）
cd /你的项目路径

# 停止旧容器
docker stop hrtools
docker rm hrtools

# 重新构建镜像
docker-compose build --no-cache

# 启动新容器
docker-compose up -d

# 查看日志
docker logs -f hrtools
```
