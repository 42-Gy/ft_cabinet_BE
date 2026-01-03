#!/bin/bash

# Cabinet Database Backup Script
# 데이터베이스를 백업하고 지정된 디렉토리에 저장합니다.

set -e

# 설정
BACKUP_DIR="./backups/database"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="cabinet_backup_${TIMESTAMP}.sql"
CONTAINER_NAME="cabi_db"
DB_USER="user"
DB_PASSWORD="rud1tks2?!"
DB_NAME="cabi"

# 백업 디렉토리 생성
mkdir -p "$BACKUP_DIR"

echo "📦 Starting database backup..."
echo "📅 Timestamp: $TIMESTAMP"

# 백업 실행
docker exec "$CONTAINER_NAME" mysqldump \
  -u"$DB_USER" \
  -p"$DB_PASSWORD" \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  "$DB_NAME" > "$BACKUP_DIR/$BACKUP_FILE"

# 백업 파일 압축
gzip "$BACKUP_DIR/$BACKUP_FILE"
BACKUP_FILE_GZ="${BACKUP_FILE}.gz"

echo "✅ Backup completed: $BACKUP_DIR/$BACKUP_FILE_GZ"
echo "📊 Backup size: $(du -h "$BACKUP_DIR/$BACKUP_FILE_GZ" | cut -f1)"

# 7일 이상 된 백업 파일 자동 삭제
echo "🧹 Cleaning up old backups (older than 7 days)..."
find "$BACKUP_DIR" -name "cabinet_backup_*.sql.gz" -mtime +7 -delete
echo "✅ Cleanup completed"

# 최근 백업 파일 목록 출력
echo ""
echo "📂 Recent backups:"
ls -lh "$BACKUP_DIR" | tail -5

exit 0

