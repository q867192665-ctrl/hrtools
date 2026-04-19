# ============================================================
# 工资签收系统 - ARM64 Docker 镜像构建脚本 (PowerShell)
# 适用于 MikroTik hAP ax2 (ARM64/aarch64)
# ============================================================

$ErrorActionPreference = "Stop"

$IMAGE_NAME = "salary-system"
$IMAGE_TAG = "latest"
$TAR_FILE = "salary-system-arm64.tar.gz"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  构建工资签收系统 ARM64 Docker 镜像" -ForegroundColor Cyan
Write-Host "  目标设备: MikroTik hAP ax2" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# 检查 Docker 是否可用
try {
    docker version | Out-Null
} catch {
    Write-Host "[ERROR] Docker 不可用，请先安装 Docker Desktop" -ForegroundColor Red
    exit 1
}

# 检查 buildx 是否可用
try {
    docker buildx version | Out-Null
} catch {
    Write-Host "[ERROR] docker buildx 不可用，请先启用 buildx 插件" -ForegroundColor Red
    exit 1
}

# 安装 QEMU 模拟器
Write-Host "[INFO] 安装 QEMU 模拟器（用于 ARM64 交叉编译）..." -ForegroundColor Yellow
docker run --privileged --rm tonistiigi/binfmt --install all

# 确保 arm64 构建器存在
$BUILDER_NAME = "arm64-builder"
$builderExists = docker buildx inspect $BUILDER_NAME 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[INFO] 创建 arm64 buildx 构建器..." -ForegroundColor Yellow
    docker buildx create --name $BUILDER_NAME --platform linux/arm64
}
docker buildx use $BUILDER_NAME

# 构建镜像
Write-Host "[INFO] 开始构建 ARM64 镜像（可能需要几分钟）..." -ForegroundColor Yellow
docker buildx build --platform linux/arm64 -t "${IMAGE_NAME}:${IMAGE_TAG}" --load .

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] 镜像构建失败！" -ForegroundColor Red
    exit 1
}

Write-Host "[INFO] 镜像构建完成！" -ForegroundColor Green
docker images "${IMAGE_NAME}:${IMAGE_TAG}"

# 导出镜像
Write-Host ""
Write-Host "[INFO] 导出镜像为 tar.gz 文件..." -ForegroundColor Yellow
docker save "${IMAGE_NAME}:${IMAGE_TAG}" | gzip > $TAR_FILE

$fileSize = (Get-Item $TAR_FILE).Length / 1MB
$fileSizeStr = "{0:N1} MB" -f $fileSize

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  构建完成！" -ForegroundColor Green
Write-Host "  镜像文件: $TAR_FILE" -ForegroundColor Green
Write-Host "  文件大小: $fileSizeStr" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "部署到 MikroTik hAP ax2:" -ForegroundColor Cyan
Write-Host "  1. scp $TAR_FILE admin@<路由器IP>:/" -ForegroundColor White
Write-Host "  2. 在路由器上: /container/add file=$TAR_FILE interface=veth1" -ForegroundColor White
Write-Host "  3. 启动容器: /container/start 0" -ForegroundColor White
Write-Host ""
Write-Host "详细部署步骤请参考: MIKROTIK_DEPLOY.md" -ForegroundColor Yellow
