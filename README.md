# 🗄️ 42Cabi Gyeongsan Ver 4.5+ (AI Edition)

> **42 경산 캠퍼스 사물함 대여/반납 서비스**<br>
> 사용자의 편의성, 공정한 이용, 그리고 **AI 기술을 활용한 쾌적한 환경 조성**을 위해 개발된 REST API 서버입니다.

<br>

## 📜 Version History (업데이트 내역)

| 버전 | 주요 변화 | 상세 내용 |
| :--- | :--- | :--- |
| **Ver 1.0** | **MVP 모델** | 기본적인 대여/반납 로직 구현, DB 비관적 락(Lock) 적용 |
| **Ver 2.0** | **보안 & 안정성** | 민감 정보 분리(`.env`), 스케줄러 N+1 문제 해결, 로깅 시스템 구축 |
| **Ver 3.0** | **아키텍처 확장** | **Spring Security + JWT** 도입 (Stateless 전환), 필터 기반 보안 구축 |
| **Ver 4.0** | **게임화 & 상점** | **제곱 패널티($D^2$)**, **아이템 상점** 구현, API 권한 최적화 |
| **Ver 4.5** | **운영 고도화** | 관리자 리팩토링(Intra ID), 반납 메모(Share Code) 기능 |
| **Ver 4.5+** | **AI & 성능 최적화** | **이사/반납 시 AI 검사 강제**, **비동기 알림(@Async)**, **DB 풀 튜닝(HikariCP)**, **API 속도 제한(RateLimiter)** |

<br>

## 🛠 Tech Stack

| 분류 | 기술 |
| :--- | :--- |
| **Backend** | Java 17, **Spring Boot 3.5.8**, Spring Security, JPA |
| **AI Server** | **Python 3.10+**, **FastAPI**, Scikit-learn, OpenCV (HOG) |
| **Database** | MariaDB 10.6, **Redis** (Token/Cache) |
| **Stability** | **Resilience4j** (CircuitBreaker, RateLimiter), **HikariCP** (Connection Pool) |
| **Infra** | Docker, Docker Compose, AWS EC2 |
| **Tools** | Gradle, Slack Webhook, **Spring Actuator** |

<br>

## 🚀 Key Features (핵심 기능)

### 1. AI 기반 반납/이사 청결도 검사 (AI Cleanliness Check) - Ver 4.5+ [UPDATED] ⭐
* **사진 인증 필수:** 사물함 **반납** 및 **이사(Swap)** 시 **내부 사진**을 반드시 첨부해야 합니다.
* **실시간 AI 분석:** 업로드된 사진은 **FastAPI AI 서버**로 전송되어, 사물함이 비어있는지(`EMPTY`) 짐이 남아있는지(`OCCUPIED`) 판별합니다.
* **반납 거부:** 짐이 감지되면 즉시 요청이 거부되며, "물품을 수거해주세요"라는 안내가 전달됩니다.
* **목적:** 쓰레기가 방치된 사물함을 다음 사용자에게 넘기는(일명 폭탄 돌리기) 문제를 원천 차단합니다.

### 2. 고성능/안정성 아키텍처 (Performance & Stability) - Ver 4.5+ [NEW]
* **비동기 처리:** 슬랙 알림 등 부가 작업은 **`@Async`** 스레드로 분리하여 메인 로직의 응답 속도를 보장합니다.
* **API 보호:** 42 Intra API 호출 시 **RateLimiter**가 초당 요청 횟수를 조절하여 IP 차단을 방지합니다.
* **DB 튜닝:** HikariCP 커넥션 풀을 최적화하여 동시 접속자가 몰려도 DB 연결이 고갈되지 않도록 방어합니다.

### 3. 블랙홀 유저 정책 변경 (Return Hold Policy)
* **변경:** 블랙홀(퇴소) 진입 시 **'반납 보류'** 상태로 전환되며, 관리자가 직접 개입하기 전까지 데이터 무결성을 유지합니다.

### 4. 스마트 반납 로직 (Smart Return)
* **반납 메모(Share Code):** AI 검사를 통과하면, 사용자가 입력한 **4자리 비밀번호**가 다음 사용자를 위한 메모로 저장됩니다. (필수 입력)

<br>

## ⚙️ Setup & Run (실행 방법)

### 1. 프로젝트 클론
```bash
git clone [https://github.com/farmer0010/42_cabinet_backend_ai.git](https://github.com/farmer0010/42_cabinet_backend_ai.git)
cd 42_cabinet_backend_ai
```

### 2. 환경 변수 설정 (For DevOps) ⚠️
보안을 위해 실제 값은 포함되어 있지 않습니다. 아래 템플릿을 참고하여 설정 파일을 생성하세요.

#### A. `.env` (Project Root) - DB 및 인프라 설정
```properties
# Database Configuration
DB_ROOT_PASSWORD=
DB_USER=
DB_PASSWORD=
TZ=Asia/Seoul
```

#### B. `src/main/resources/secret.properties` - 애플리케이션 시크릿
```properties
# Database Connection
spring.datasource.username=
spring.datasource.password=

# OAuth2 (42 API)
spring.security.oauth2.client.registration.42.client-id=
spring.security.oauth2.client.registration.42.client-secret=

# JWT
jwt.secret=

# Slack Notification
SLACK_BOT_TOKEN=

# AI Server Connection (FastAPI)
ai.server.url=
```

### 3. 실행 (Docker Compose)
```bash
# 1. DB & Redis 실행
docker-compose up -d

# 2. AI 서버 실행 (Python 환경 필요)
cd ai_server
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000

# 3. 백엔드 서버 실행
./gradlew bootRun
```

<br>

## 🧪 API Usage (Updated)

* **Base URL:** `http://localhost:8080`
* **중요 변경 사항:** 반납 및 이사 API는 **Multipart/form-data** 필수입니다.

### 📦 Cabinet Lent & Return (AI Integrated)

#### 1. 사물함 대여
* **URL:** `POST /v4/lent/cabinets/{visibleNum}`

#### 2. 사물함 반납 (Return)
* **URL:** `POST /v4/lent/return`
* **Content-Type:** `multipart/form-data`
* **Body:**
    * `file`: **(Required)** 사물함 내부 촬영 이미지 파일 (`.jpg`, `.png`)
    * `shareCode`: **(Required)** 4자리 숫자 비밀번호 (String, 예: "1234")

#### 3. 사물함 이사 (Swap) [NEW]
* **URL:** `POST /v4/lent/swap/{newVisibleNum}`
* **Content-Type:** `multipart/form-data`
* **Body:**
    * `file`: **(Required)** **기존 사물함** 내부 촬영 이미지 파일
    * `shareCode`: **(Required)** **기존 사물함**의 4자리 비밀번호
* **Response:**
    * `200 OK`: "✅ AI 검사 통과! 사물함 이사 완료!"
    * `400 Bad Request`: "사물함 안에 물품이 감지되었습니다." (AI 판독)

### 👤 User & Auth
* **로그인:** `GET /oauth2/authorization/42`
* **내 정보 조회:** `GET /v4/users/me`

### ⚙️ Admin Actions
* **유저 검색 (Intra ID):** `GET /v4/admin/users/{name}`
* **강제 반납:** `POST /v4/admin/cabinets/{visibleNum}/force-return`

<br>

## 📂 Project Structure

```text
.
├── .github
│   └── workflows
│       └── gradle.yml             # Github Actions CI/CD 설정
├── .env                           # [Secret] DB 및 TimeZone 환경 변수
├── build.gradle                   # 의존성 설정 (Spring Cloud, Resilience4j 등)
├── docker-compose.yaml            # MariaDB, Redis 컨테이너 설정
├── src
│   ├── main
│   │   ├── java/com/gyeongsan/cabinet
│   │   │   ├── CabinetApplication.java  # @EnableAsync, @EnableScheduling
│   │   │   │
│   │   │   ├── admin                  # [Admin] 관리자 기능
│   │   │   │   ├── controller/AdminController.java
│   │   │   │   ├── dto/AdminUserDetailResponse.java
│   │   │   │   └── service/AdminService.java
│   │   │   │
│   │   │   ├── alarm                  # [Alarm] 슬랙 알림 시스템
│   │   │   │   ├── dto/AlarmEvent.java
│   │   │   │   ├── AlarmEventHandler.java      # @Async 비동기 리스너
│   │   │   │   └── SlackBotService.java
│   │   │   │
│   │   │   ├── auth                   # [Auth] 인증 및 보안 (JWT/OAuth2)
│   │   │   │   ├── config/SecurityConfig.java
│   │   │   │   ├── controller/AuthController.java
│   │   │   │   ├── domain/UserPrincipal.java
│   │   │   │   ├── jwt/
│   │   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   │   └── JwtTokenProvider.java
│   │   │   │   ├── oauth/
│   │   │   │   │   ├── CustomOAuth2UserService.java
│   │   │   │   │   └── OAuth2SuccessHandler.java
│   │   │   │   └── service/AuthService.java
│   │   │   │
│   │   │   ├── cabinet                # [Cabinet] 사물함 도메인
│   │   │   │   ├── controller/CabinetController.java
│   │   │   │   ├── domain/Cabinet.java
│   │   │   │   ├── dto/CabinetDetailResponseDto.java
│   │   │   │   ├── repository/CabinetRepository.java
│   │   │   │   └── service/CabinetService.java
│   │   │   │
│   │   │   ├── global                 # [Global] 공통 설정 및 예외 처리
│   │   │   │   ├── aspect/LoggingAspect.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── RedisConfig.java
│   │   │   │   │   └── WebConfig.java
│   │   │   │   └── exception/
│   │   │   │       ├── ErrorCode.java
│   │   │   │       ├── GlobalExceptionHandler.java
│   │   │   │       └── ServiceException.java
│   │   │   │
│   │   │   ├── item                   # [Item] 상점 및 아이템
│   │   │   │   ├── controller/StoreController.java
│   │   │   │   ├── domain/
│   │   │   │   │   ├── Item.java
│   │   │   │   │   └── ItemHistory.java
│   │   │   │   ├── repository/ItemRepository.java
│   │   │   │   └── service/
│   │   │   │       ├── ItemPriceInitializer.java
│   │   │   │       └── StoreService.java
│   │   │   │
│   │   │   ├── lent                   # [Lent] 대여/반납 (핵심 로직)
│   │   │   │   ├── controller/LentController.java  # Multipart (사진 업로드)
│   │   │   │   ├── domain/LentHistory.java
│   │   │   │   ├── dto/LentReturnRequest.java
│   │   │   │   ├── repository/LentRepository.java
│   │   │   │   └── service/LentFacadeService.java  # AI 검사 & 트랜잭션 분리
│   │   │   │
│   │   │   ├── user                   # [User] 사용자 및 스케줄러
│   │   │   │   ├── controller/UserController.java
│   │   │   │   ├── domain/User.java
│   │   │   │   ├── repository/UserRepository.java
│   │   │   │   ├── scheduler/
│   │   │   │   │   ├── BlackholeScheduler.java
│   │   │   │   │   └── LogtimeScheduler.java
│   │   │   │   └── service/UserService.java
│   │   │   │
│   │   │   └── utils                  # [Utils] 유틸리티
│   │   │       └── FtApiManager.java               # 42 Intra API 연동 (@RateLimiter)
│   │   │
│   │   └── resources
│   │       ├── application.yml        # 메인 설정 (HikariCP, RateLimiter 등)
│   │       ├── logback-spring.xml     # 로그 설정
│   │       ├── secret.properties      # [Secret] API 키 관리
│   │       └── static/index.html      # 테스트용 프론트 페이지
│   │
│   └── test                           # 테스트 코드
│       └── java/com/gyeongsan/cabinet
│           └── CabinetApplicationTests.java
```
