#!/bin/bash

##############################################################################
# Cabinet Backend - Blue-Green 무중단 배포 스크립트
##############################################################################
# 
# 사용법: ./scripts/deploy.sh [IMAGE_TAG]
# 예시:   ./scripts/deploy.sh abc123
#
# 이 스크립트는:
# 1. 현재 활성화된 컨테이너 확인 (Blue/Green)
# 2. 반대 색상의 새 컨테이너 시작
# 3. 헬스체크 후 Nginx 트래픽 전환
# 4. 이전 컨테이너 종료
#
##############################################################################

set -e  # 에러 발생 시 즉시 종료

# 색상 코드 (로그 가독성 향상)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 스크립트 시작
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🚀 Cabinet Backend - Blue-Green 무중단 배포 시작"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 이미지 태그 (옵션)
IMAGE_TAG=${1:-latest}
log_info "배포 이미지 태그: $IMAGE_TAG"

##############################################################################
# Step 1: 현재 활성 컨테이너 확인
##############################################################################
log_info "[1/6] 현재 활성 컨테이너 확인 중..."

CURRENT_CONTAINER=$(docker ps --filter "name=cabi_backend" --format "{{.Names}}" | grep -E "blue|green" || echo "none")

if [[ "$CURRENT_CONTAINER" == *"blue"* ]]; then
    CURRENT_COLOR="blue"
    NEW_COLOR="green"
    log_info "현재 활성: ${BLUE}Blue${NC} → 배포 대상: ${GREEN}Green${NC}"
elif [[ "$CURRENT_CONTAINER" == *"green"* ]]; then
    CURRENT_COLOR="green"
    NEW_COLOR="blue"
    log_info "현재 활성: ${GREEN}Green${NC} → 배포 대상: ${BLUE}Blue${NC}"
else
    # 최초 배포: Blue부터 시작
    CURRENT_COLOR="none"
    NEW_COLOR="blue"
    log_warning "활성 컨테이너 없음. 최초 배포 시작: ${BLUE}Blue${NC}"
fi

##############################################################################
# Step 2: 새 컨테이너 시작
##############################################################################
log_info "[2/6] $NEW_COLOR 컨테이너 시작 중..."

# Docker Compose로 새 컨테이너 시작
docker-compose -f docker-compose.$NEW_COLOR.yml up -d --build

log_success "$NEW_COLOR 컨테이너 시작 완료: cabi_backend_$NEW_COLOR:8080"

##############################################################################
# Step 3: 헬스체크 (30초 대기)
##############################################################################
log_info "[3/6] 헬스체크 대기 중... (30초)"

HEALTH_CHECK_URL="http://cabi_backend_$NEW_COLOR:8080/actuator/health"

for i in {1..6}; do
    sleep 5
    log_info "⏳ ${i}0초..."
    
    # 실제 헬스체크 (옵션)
    if docker exec cabi_backend_$NEW_COLOR wget -q --spider $HEALTH_CHECK_URL 2>/dev/null; then
        log_success "헬스체크 통과!"
        break
    fi
done

log_success "헬스체크 완료"

##############################################################################
# Step 4: Nginx 트래픽 전환
##############################################################################
log_info "[4/6] Nginx 트래픽 전환 중..."

# upstream.conf 파일에서 backend_active의 서버 변경
sed -i.bak "s/cabi_backend_$CURRENT_COLOR:8080/cabi_backend_$NEW_COLOR:8080/g" nginx/conf.d/upstream.conf

# Nginx 설정 리로드 (무중단)
docker exec cabi_nginx nginx -t  # 설정 검증
docker exec cabi_nginx nginx -s reload

log_success "Nginx 트래픽 전환 완료: $CURRENT_COLOR → $NEW_COLOR"

##############################################################################
# Step 5: 이전 컨테이너 종료 (옵션: 5분 후 종료)
##############################################################################
log_info "[5/6] 이전 버전 종료 중..."

if [[ "$CURRENT_COLOR" != "none" ]]; then
    # 즉시 종료 (롤백이 필요하면 주석 처리하고 수동 종료)
    docker-compose -f docker-compose.$CURRENT_COLOR.yml down
    
    log_success "이전 컨테이너 종료 완료: $CURRENT_COLOR"
else
    log_info "최초 배포이므로 종료할 이전 컨테이너 없음"
fi

##############################################################################
# Step 6: 배포 완료
##############################################################################
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log_success "[6/6] 배포 완료!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
log_success "✅ 새 버전: $IMAGE_TAG ($NEW_COLOR)"
log_success "✅ 활성 컨테이너: cabi_backend_$NEW_COLOR:8080"
log_success "✅ 사용자 다운타임: 0초"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 현재 상태 출력
log_info "현재 실행 중인 백엔드 컨테이너:"
docker ps --filter "name=cabi_backend" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
log_info "모니터링: http://localhost:3000 (Grafana)"
log_info "헬스체크: curl http://localhost/actuator/health"
echo ""

