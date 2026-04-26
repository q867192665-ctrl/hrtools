# ============================================================
# 人事管理系统 - Docker 镜像 (ARM64)
# ============================================================

FROM python:3.11-slim-bookworm

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    TZ=Asia/Shanghai \
    FLASK_APP=app.py \
    DATABASE_PATH=/app/database/salary_system.db \
    SIGNATURE_DIR=/app/backend/signatures

RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY backend/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY backend/app.py /app/backend/app.py
COPY backend/auth.py /app/backend/auth.py
COPY backend/salary_manager.py /app/backend/salary_manager.py
COPY backend/signature_manager.py /app/backend/signature_manager.py
COPY backend/data_manager.py /app/backend/data_manager.py
COPY backend/device_manager.py /app/backend/device_manager.py
COPY backend/templates/ /app/backend/templates/
COPY logo.png /app/logo.png

COPY database/schema.sql /app/database/schema.sql

RUN mkdir -p /app/backend/signatures \
    /app/backend/exports \
    /app/backend/customer_uploads/exports \
    /app/database

COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

RUN echo "=== 检查文件 ===" \
    && ls -la /app/backend/ \
    && ls -la /app/backend/templates/ \
    && ls -la /app/database/ \
    && echo "=== 文件检查完成 ==="

EXPOSE 32996

VOLUME ["/app/database", "/app/backend/signatures", "/app/backend/exports", "/app/backend/customer_uploads"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:32996/api/health')" || exit 1

ENTRYPOINT ["/app/entrypoint.sh"]

CMD ["gunicorn", "--bind", "[::]:32996", "--workers", "2", "--timeout", "120", "--access-logfile", "-", "app:app"]
