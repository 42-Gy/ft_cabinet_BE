# 🚀 Cabinet CI/CD 사용 가이드 (간단 버전)

**작성일**: 2026-01-03  
**DevOps**: @ahnhyunjun

---

## 📋 CI/CD 구조

### ✅ **CI (Continuous Integration) - 자동**
```
PR 생성/업데이트 → 빌드 → 테스트 → 이미지 생성 → 저장
```
- **파일**: `.github/workflows/ci.yml`
- **트리거**: PR 생성, develop 브랜치 push
- **역할**: 코드 검증

### ✅ **CD (Continuous Deployment) - 수동**
```
승인 버튼 클릭 → 서버 접속 → Blue-Green 배포 → 완료
```
- **파일**: `.github/workflows/cd.yml`
- **트리거**: 수동 실행 (승인 필요)
- **역할**: 프로덕션 배포

---

## 🎯 **실제 사용 방법**

### **새 기능 개발 → 배포**

#### **1. 개발 & PR**
```bash
# 브랜치 생성
git checkout -b feature/new-feature

# 코드 작성
# ... 작업 ...

# 커밋 & 푸시
git add .
git commit -m "feat: 새 기능 추가"
git push origin feature/new-feature
```

#### **2. GitHub에서 PR 생성**
```
GitHub → Pull requests → New pull request
→ feature/new-feature → main
→ Create pull request
```

**→ 이 순간 CI 자동 실행! ✅**

#### **3. CI 결과 확인**
```
PR 페이지 → Checks 탭
✅ CI - Build & Test
  ├─ Build with Gradle
  ├─ Test Results
  └─ Docker Image Push
```

**모두 ✅ 통과하면 Merge 가능!**

#### **4. Merge**
```
PR 승인 → Merge pull request
```

#### **5. 배포 (수동)**
```
GitHub → Actions → CD - Deploy to Production
→ Run workflow 클릭

입력:
- Branch: main
- image_tag: latest (또는 특정 커밋 해시)
- environment: production

→ Run workflow 버튼 클릭
```

**→ 배포 시작! 🚀**

---

## 🔧 **GitHub Secrets 설정 (최초 1회)**

```
GitHub Repository → Settings → Secrets and variables → Actions
```

**필수 3개**:
1. `SERVER_HOST`: 서버 IP (예: 123.456.789.0)
2. `SERVER_USER`: SSH 사용자명 (예: ubuntu)
3. `SSH_PRIVATE_KEY`: SSH 개인키 (전체 내용)

**상세 가이드**: `GITHUB_SECRETS_GUIDE.md` 참고

---

## 🧪 **테스트 방법**

### **1. CI 테스트 (자동)**
```bash
# develop 브랜치에서
echo "# Test CI" >> README.md
git add README.md
git commit -m "test: CI 테스트"
git push origin develop
```

**→ GitHub Actions에서 CI 자동 실행 확인**

### **2. CD 테스트 (수동)**
```
GitHub Actions → CD - Deploy to Production
→ Run workflow
  - image_tag: latest
  - environment: production
→ Run workflow
```

**→ 배포 진행 상황 실시간 확인**

---

## 🔄 **롤백 방법**

### **방법 1: CD로 이전 버전 배포**
```
CD workflow 실행
→ image_tag: <이전 커밋 해시> 입력
→ 배포 실행
```

### **방법 2: 서버에서 수동 롤백**
```bash
ssh user@server
cd /home/user/42_Cabinet/ft_cabinet_BE

# Nginx 전환
sed -i 's/green/blue/g' nginx/conf.d/upstream.conf
docker exec cabi_nginx nginx -s reload
```

---

## ⚠️ **주의사항**

1. **main 브랜치는 자동 배포 안됨**
   - Merge 후 CD를 수동으로 실행해야 배포됨

2. **develop 브랜치는 CI만 실행**
   - 빌드/테스트만 하고 배포는 안함

3. **Docker 권한 필요**
   ```bash
   sudo usermod -aG docker $USER
   ```

---

## 📊 **배포 체크리스트**

### **배포 전**
- [ ] PR이 main에 Merge 되었는가?
- [ ] CI가 모두 통과했는가?
- [ ] 테스트가 성공했는가?
- [ ] 팀원들에게 알렸는가?

### **배포 중**
- [ ] CD workflow 실행
- [ ] 이미지 태그 확인
- [ ] environment: production 선택
- [ ] 배포 진행 상황 모니터링

### **배포 후**
- [ ] 헬스체크 확인: `curl http://localhost/actuator/health`
- [ ] 주요 API 테스트
- [ ] Grafana 모니터링: http://localhost:3000
- [ ] 에러 로그 확인: `docker logs cabi_backend_green`

---

## 🆘 **문제 해결**

### **CI 실패**
```
빌드 실패 → 로그 확인 → 코드 수정 → 다시 푸시
테스트 실패 → 테스트 로그 확인 → 수정 → 다시 푸시
```

### **CD 실패**
```
SSH 접속 실패 → GITHUB_SECRETS_GUIDE.md 참고
배포 실패 → 서버 로그 확인
502 에러 → 헬스체크 확인, 롤백 고려
```

---

## 📚 **참고 문서**

1. **CICD_GUIDE.md** - 전체 상세 가이드
2. **GITHUB_SECRETS_GUIDE.md** - Secret 설정 방법
3. **OPTIMIZATION_REPORT.md** - 성능 최적화 내역

---

## 📞 **문의**

- **DevOps**: @ahnhyunjun
- **문제 발생 시**: GitHub Issues 생성

---

**마지막 업데이트**: 2026-01-03 21:40 KST

