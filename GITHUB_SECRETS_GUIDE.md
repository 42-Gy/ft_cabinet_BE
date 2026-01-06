# 🔐 GitHub Secrets 설정 가이드

CI/CD 파이프라인 실행을 위해 다음 Secret을 GitHub Repository에 등록해야 합니다.

## 📍 Secret 등록 위치

```
GitHub Repository → Settings → Secrets and variables → Actions → New repository secret
```

---

## 🔑 필수 Secrets

### 1. **SSH 접속 정보**

#### `SERVER_HOST`
- **설명**: 배포 서버의 IP 주소 또는 도메인
- **예시**: `123.456.789.0` 또는 `cabinet.42gy.kr`

#### `SERVER_USER`
- **설명**: SSH 접속 사용자명
- **예시**: `ubuntu` 또는 `cabinet`

#### `SSH_PRIVATE_KEY`
- **설명**: SSH 개인키 (Private Key)
- **생성 방법**:
  ```bash
  # 서버에서 SSH 키 생성
  ssh-keygen -t rsa -b 4096 -C "github-actions@cabinet"
  
  # 공개키를 authorized_keys에 추가
  cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
  
  # 개인키 내용을 Secret에 복사
  cat ~/.ssh/id_rsa
  ```

#### `SERVER_PORT` (옵션)
- **설명**: SSH 포트 (기본값: 22)
- **예시**: `22` 또는 `2222`

---

### 2. **배포 경로**

#### `DEPLOY_PATH` (옵션)
- **설명**: 서버의 프로젝트 절대 경로
- **기본값**: `/home/user/42_Cabinet/ft_cabinet_BE`
- **예시**: `/home/ubuntu/cabinet/backend`

---

## 📋 Secret 체크리스트

### 최소 필수
- [ ] `SERVER_HOST`
- [ ] `SERVER_USER`
- [ ] `SSH_PRIVATE_KEY`

### 권장 설정 (옵션)
- [ ] `SERVER_PORT` (기본값: 22)
- [ ] `DEPLOY_PATH` (기본값: /home/user/42_Cabinet/ft_cabinet_BE)

---

## 🧪 테스트 방법

### 1. SSH 접속 테스트
```bash
ssh -i ~/.ssh/id_rsa $SERVER_USER@$SERVER_HOST -p $SERVER_PORT
```

### 2. 배포 경로 확인
```bash
ssh $SERVER_USER@$SERVER_HOST "ls -la $DEPLOY_PATH"
```

### 3. Docker 권한 확인
```bash
ssh $SERVER_USER@$SERVER_HOST "docker ps"
```

---

## ⚠️ 주의사항

1. **Secret은 절대 Git에 커밋하지 마세요!**
2. **SSH 키는 암호 없이 생성하세요** (GitHub Actions 자동 실행)
3. **서버 사용자가 Docker 권한을 가지고 있어야 합니다**:
   ```bash
   sudo usermod -aG docker $USER
   ```

---

## 🔍 Secret 확인

등록된 Secret 목록 확인:
```
GitHub Repository → Settings → Secrets and variables → Actions
```

---

## 📞 문의

Secret 설정 중 문제가 발생하면 DevOps 담당자에게 문의하세요.

