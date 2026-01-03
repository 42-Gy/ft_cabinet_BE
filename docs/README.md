# 📚 Cabinet Backend - Documentation

이 폴더는 Cabinet 백엔드 프로젝트의 **DevOps 및 최적화 관련 문서**를 포함합니다.

---

## 📋 문서 목록

### 🌟 **종합 가이드**

#### 0. [DEVOPS_COMPLETE_GUIDE.md](./DEVOPS_COMPLETE_GUIDE.md) ⭐
**프로젝트 DevOps 전체 여정 - 시작부터 끝까지**

- 프로젝트 개요 및 아키텍처
- 초기 상태 분석 & 문제점 진단
- 인프라 구축 전 과정
- 성능 최적화 전체 내역 (6배 향상)
- CI/CD 파이프라인 구축
- **완전한 테스트 가이드** (로컬/CI/CD/배포)
- 배포 프로세스 & 체크리스트
- 모니터링 & 운영 가이드
- 트러블슈팅 & 긴급 상황 대응
- 전체 체크리스트

**대상**: 전체 팀원 (필독! 프로젝트 전체 이해)  
**분량**: 약 1,500줄  
**특징**: 이 한 문서로 DevOps 전체를 이해할 수 있음

---

### 🚀 **CI/CD 관련**

#### 1. [CICD_GUIDE.md](./CICD_GUIDE.md)
**CI/CD 파이프라인 완전 가이드**

- GitHub Actions 기반 CI/CD 전체 아키텍처
- Blue-Green 배포 전략 상세 설명
- 다른 도구들(Jenkins, GitLab CI, Docker Swarm 등)과의 비교
- 워크플로우 설계 원리 및 동작 방식
- 상세한 구현 내용 및 트러블슈팅

**대상**: 전체 팀원 (CI/CD 시스템 이해 필요)  
**분량**: 약 1,000줄

---

#### 2. [CICD_QUICKSTART.md](./CICD_QUICKSTART.md)
**CI/CD 빠른 시작 가이드**

- CI/CD 사용법 빠른 참조
- 실전 시나리오별 가이드
- 주요 명령어 및 체크리스트
- 문제 해결 빠른 참조

**대상**: 개발자 (실제 사용법만 빠르게 확인)  
**분량**: 약 200줄

---

#### 3. [GITHUB_SECRETS_GUIDE.md](./GITHUB_SECRETS_GUIDE.md)
**GitHub Secrets 설정 가이드**

- CI/CD에 필요한 GitHub Secrets 목록
- 각 Secret의 용도 및 생성 방법
- SSH 키 생성 및 등록 방법
- 보안 주의사항

**대상**: DevOps 담당자, Repository Admin  
**분량**: 약 110줄

---

### ⚡ **성능 최적화 관련**

#### 4. [OPTIMIZATION_REPORT2.md](./OPTIMIZATION_REPORT2.md)
**성능 최적화 보고서**

- 데이터베이스 쿼리 최적화 (인덱싱)
- Nginx 최적화 (Gzip, Keep-Alive)
- HikariCP 커넥션 풀 튜닝
- N+1 쿼리 문제 분석 및 해결
- 성능 개선 결과 및 테스트 방법

**대상**: 백엔드 개발자, DevOps 담당자  
**분량**: 약 400줄

---

### 🔧 **DevOps 개선 사항**

#### 5. [DEVOPS_IMPROVEMENTS1.md](./DEVOPS_IMPROVEMENTS1.md)
**DevOps 개선 사항 보고서**

- 프로젝트 초기 상태 분석
- 완료된 개선 사항
  - Prometheus 설정 수정
  - 데이터베이스 백업 자동화
  - 로그 관리 설정
- 향후 개선 계획
- 보안 체크리스트

**대상**: DevOps 담당자, 프로젝트 매니저  
**분량**: 약 260줄

---

## 📊 문서 읽기 순서 추천

### **🌟 처음 시작하는 분 (필수!)**
```
1. DEVOPS_COMPLETE_GUIDE.md ← ⭐ 여기서 시작!
   (프로젝트 전체 이해, 테스트 방법 포함)
```

### **신규 팀원 온보딩**
```
1. DEVOPS_COMPLETE_GUIDE.md (전체 DevOps 이해) ← 필수!
2. CICD_QUICKSTART.md (CI/CD 사용법)
3. OPTIMIZATION_REPORT2.md (성능 최적화 현황)
```

### **DevOps 담당자**
```
1. DEVOPS_COMPLETE_GUIDE.md (전체 여정 이해) ← 필수!
2. CICD_GUIDE.md (CI/CD 시스템 심층 이해)
3. GITHUB_SECRETS_GUIDE.md (배포 설정)
4. OPTIMIZATION_REPORT2.md (성능 모니터링)
5. DEVOPS_IMPROVEMENTS1.md (개선 이력)
```

### **백엔드 개발자**
```
1. DEVOPS_COMPLETE_GUIDE.md (인프라 이해) ← 권장!
2. OPTIMIZATION_REPORT2.md (쿼리 최적화 이해)
3. CICD_QUICKSTART.md (CI/CD 사용법)
```

### **빠른 참조용**
```
테스트 방법: DEVOPS_COMPLETE_GUIDE.md (섹션 6)
CI/CD 사용법: CICD_QUICKSTART.md
배포 설정: GITHUB_SECRETS_GUIDE.md
성능 이슈: OPTIMIZATION_REPORT2.md
전체 체크리스트: DEVOPS_COMPLETE_GUIDE.md (섹션 10)
```

---

## 🗂️ 폴더 구조

```
Docs/
├── README.md                       ← 이 문서 (문서 인덱스)
├── DEVOPS_COMPLETE_GUIDE.md       ← ⭐ 종합 가이드 (필독!)
├── CICD_GUIDE.md                  ← CI/CD 완전 가이드
├── CICD_QUICKSTART.md             ← CI/CD 빠른 시작
├── GITHUB_SECRETS_GUIDE.md        ← GitHub Secrets 설정
├── OPTIMIZATION_REPORT2.md        ← 성능 최적화 보고서
└── DEVOPS_IMPROVEMENTS1.md        ← DevOps 개선 사항
```

---

## 🔗 관련 문서

프로젝트 루트의 다른 중요 문서들:

- **[../README.md](../README.md)**: 프로젝트 전체 개요 및 설정 가이드
- **[../docker-compose.yaml](../docker-compose.yaml)**: 개발 환경 Docker 설정
- **[../docker-compose.blue.yml](../docker-compose.blue.yml)**: Blue 배포 환경
- **[../docker-compose.green.yml](../docker-compose.green.yml)**: Green 배포 환경
- **[../scripts/deploy.sh](../scripts/deploy.sh)**: Blue-Green 배포 스크립트

---

## 📝 문서 업데이트 이력

| 날짜 | 문서 | 내용 |
|------|------|------|
| 2026-01-03 | DEVOPS_COMPLETE_GUIDE.md | ⭐ 종합 가이드 작성 (전체 여정) |
| 2026-01-03 | CICD_GUIDE.md | GitHub Actions 기반 CI/CD 초기 작성 |
| 2026-01-03 | CICD_QUICKSTART.md | CI/CD 빠른 시작 가이드 작성 |
| 2026-01-03 | GITHUB_SECRETS_GUIDE.md | Secrets 설정 가이드 작성 |
| 2026-01-03 | OPTIMIZATION_REPORT2.md | 성능 최적화 보고서 작성 |
| 2026-01-03 | DEVOPS_IMPROVEMENTS1.md | DevOps 개선 사항 정리 |
| 2026-01-03 | README.md | 문서 인덱스 생성 & 업데이트 |

---

## 💡 기여 가이드

새로운 문서를 추가하거나 기존 문서를 수정한 경우:

1. 이 README.md의 **문서 목록**에 추가
2. **문서 업데이트 이력** 테이블 업데이트
3. 필요시 **읽기 순서 추천** 섹션 수정

---

## 🔍 빠른 검색

**키워드별 문서 찾기**:

- **전체 가이드**: DEVOPS_COMPLETE_GUIDE.md ⭐
- **테스트 방법**: DEVOPS_COMPLETE_GUIDE.md (섹션 6)
- **체크리스트**: DEVOPS_COMPLETE_GUIDE.md (섹션 10)
- **CI/CD**: CICD_GUIDE.md, CICD_QUICKSTART.md, DEVOPS_COMPLETE_GUIDE.md
- **배포**: CICD_GUIDE.md, GITHUB_SECRETS_GUIDE.md, DEVOPS_COMPLETE_GUIDE.md
- **Blue-Green**: CICD_GUIDE.md, DEVOPS_COMPLETE_GUIDE.md
- **성능**: OPTIMIZATION_REPORT2.md, DEVOPS_COMPLETE_GUIDE.md
- **데이터베이스**: OPTIMIZATION_REPORT2.md, DEVOPS_COMPLETE_GUIDE.md
- **Nginx**: OPTIMIZATION_REPORT2.md, DEVOPS_COMPLETE_GUIDE.md
- **Docker**: CICD_GUIDE.md, DEVOPS_IMPROVEMENTS1.md, DEVOPS_COMPLETE_GUIDE.md
- **보안**: GITHUB_SECRETS_GUIDE.md, DEVOPS_IMPROVEMENTS1.md
- **모니터링**: DEVOPS_IMPROVEMENTS1.md, DEVOPS_COMPLETE_GUIDE.md
- **백업**: DEVOPS_IMPROVEMENTS1.md, DEVOPS_COMPLETE_GUIDE.md
- **트러블슈팅**: DEVOPS_COMPLETE_GUIDE.md (섹션 9)

---

## 📧 문의

문서 관련 문의사항이나 개선 제안은 팀 DevOps 담당자에게 연락하세요.

---

**마지막 업데이트**: 2026-01-03  
**문서 버전**: 1.0.0

