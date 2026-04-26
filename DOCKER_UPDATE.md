# Docker 部署更新指南

## 更新内容
- 修复Excel导入时E1列（是否代扣社保）读取错误
- 修复Excel公式计算数据读取错误

## 环境信息
- **宿主机**: homeassistant (Linux)
- **容器名**: hrtools
- **访问地址**: http://homeassistant:32996

## Linux 环境更新（推荐）

### 方式1: 使用更新脚本
```bash
chmod +x update.sh
./update.sh
```

### 方式2: 手动执行命令
```bash
# 1. 停止并删除旧容器
docker stop hrtools
docker rm hrtools

# 2. 重新构建镜像（使用最新代码）
docker-compose build --no-cache

# 3. 启动新容器
docker-compose up -d

# 4. 查看日志确认启动成功
docker logs -f hrtools
```

## 验证更新

1. 访问系统：http://homeassistant:32996
2. 使用之前的Excel文件重新导入
3. 检查：
   - E1列（是否代扣社保）是否正确读取
   - 包含公式的单元格是否正确读取计算结果

## 常用命令

```bash
# 查看实时日志
docker logs -f hrtools

# 查看最近100行日志
docker logs --tail=100 hrtools

# 查看错误日志
docker logs hrtools 2>&1 | grep -i error

# 停止容器
docker stop hrtools

# 启动容器
docker start hrtools

# 重启容器
docker restart hrtools

# 查看容器状态
docker ps | grep hrtools

# 进入容器
docker exec -it hrtools /bin/bash
```

## 回滚（如有问题）

如果更新后出现问题，可以回滚：

```bash
# 停止当前容器
docker stop hrtools
docker rm hrtools

# 使用之前的镜像重新启动（如果有备份）
docker-compose up -d
```

## 注意事项

1. **数据不会丢失**：数据库文件存储在Docker volume中，更新不会影响数据
2. **构建时间**：首次构建可能需要几分钟，请耐心等待
3. **端口占用**：确保32996端口未被其他程序占用
4. **Docker权限**：确保当前用户有Docker执行权限（可能需要sudo）
