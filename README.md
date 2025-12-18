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
| **Ver 4.5+** | **AI & 정책** | **AI 사물함 청결도 검사**, **블랙홀 반납 보류 정책**, **Multipart API 전환** |

<br>

## 🛠 Tech Stack

| 분류 | 기술 |
| :--- | :--- |
| **Backend** | Java 17, **Spring Boot 3.5.8**, Spring Security, JPA |
| **AI Server** | **Python 3.10+**, **FastAPI**, Scikit-learn, OpenCV (HOG) |
| **Database** | MariaDB 10.6, **Redis** (Token/Cache) |
| **Infra** | Docker, Docker Compose, AWS EC2 |
| **Tools** | Gradle, Slack Webhook, **Spring Actuator** |

<br>

## 🚀 Key Features (핵심 기능)

### 1. AI 기반 반납 청결도 검사 (AI Cleanliness Check) - Ver 4.5+ [NEW] ⭐
* **사진 인증 반납:** 사물함 반납 시 **내부 사진**을 반드시 첨부해야 합니다.
* **실시간 AI 분석:** 업로드된 사진은 **FastAPI AI 서버**로 전송되어, 사물함이 비어있는지(`EMPTY`) 짐이 남아있는지(`OCCUPIED`) 판별합니다.
* **반납 거부:** 짐이 감지되면 즉시 반납이 거부되며, "물품을 수거해주세요"라는 안내가 전달됩니다. 이를 통해 다음 사용자가 쓰레기가 방치된 사물함을 받는 문제를 원천 차단합니다.

### 2. 블랙홀 유저 정책 변경 (Return Hold Policy) - Ver 4.5+ [UPDATED]
* **기존:** 블랙홀(퇴소) 진입 시 시스템이 자동으로 반납 처리 (물품 방치 위험 존재).
* **변경:** 블랙홀 진입 시 **'반납 보류'** 상태로 전환되며, 유저에게 **"짐을 수거하고 직접 반납하세요"**라는 강력한 알림(Slack/Mail)을 발송합니다.
* **목적:** 관리자가 직접 개입하기 전까지 데이터 무결성을 유지하고 물품 분실 사고를 예방합니다.

### 3. 상점 및 아이템 시스템 (Shop & Item)
* **아이템 상점:** 출석체크 보상 코인으로 연장권, 이사권, 감면권 구매 가능.
* **월간 보상:** 월 50시간 이상 학습 시 **대여권(LENT)** 자동 지급.

### 4. 스마트 반납 로직 (Smart Return)
* **반납 메모(Share Code):** AI 검사를 통과하면, 사용자가 입력한 비밀번호가 **다음 사용자를 위한 메모**로 저장됩니다.

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
# Local: http://localhost:8000/predict
# Remote: [https://your-ngrok-url.ngrok-free.app/predict](https://your-ngrok-url.ngrok-free.app/predict)
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

### 📦 Cabinet Lent & Return (AI Integrated)
기존 JSON 방식에서 **Multipart/form-data** 방식으로 변경되었습니다.

* **사물함 대여:** `POST /v4/lent/cabinets/{visibleNum}`
* **사물함 반납 (NEW):** `POST /v4/lent/return`
    * **Content-Type:** `multipart/form-data`
    * **Body:**
        * `file`: (Required) 사물함 내부 촬영 이미지 파일 (`.jpg`, `.png`)
        * `shareCode`: (Optional) 다음 사용자를 위한 비밀번호 (Text)
    * **Response:**
        * `200 OK`: 반납 성공 (AI 검사 통과)
        * `400 Bad Request`: **"사물함 안에 물품이 감지되었습니다."** (AI 판독 결과)
        * `500 Internal Server Error`: **"AI 서버 오류. 관리자에게 문의하세요."** (서버 장애 시)

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
├── build.gradle                   # Gradle 의존성 및 플러그인 설정
├── docker-compose.yaml            # MariaDB, Redis 컨테이너 설정
├── src
│   ├── main
│   │   ├── java/com/gyeongsan/cabinet
│   │   │   ├── CabinetApplication.java  # Spring Boot 메인 실행 파일
│   │   │   │
│   │   │   ├── admin              # [Admin Domain] 관리자 기능
│   │   │   │   ├── controller     # AdminController (API 엔드포인트)
│   │   │   │   ├── service        # AdminService (유저 관리, 사물함 상태 변경, 강제 반납)
│   │   │   │   └── dto            # AdminUserDetailResponse, CoinProvideRequest 등
│   │   │   │
│   │   │   ├── auth               # [Auth Domain] 인증 및 인가
│   │   │   │   ├── controller     # AuthController (토큰 재발급)
│   │   │   │   ├── domain         # UserPrincipal (Security User 객체)
│   │   │   │   ├── jwt            # JwtTokenProvider, JwtAuthenticationFilter
│   │   │   │   └── oauth          # CustomOAuth2UserService, OAuth2SuccessHandler
│   │   │   │
│   │   │   ├── cabinet            # [Cabinet Domain] 사물함 관리
│   │   │   │   ├── controller     # CabinetController (현황 조회)
│   │   │   │   ├── domain         # Cabinet (Entity), CabinetStatus, LentType (Enum)
│   │   │   │   ├── repository     # CabinetRepository
│   │   │   │   └── service        # CabinetFacadeService, CabinetService
│   │   │   │
│   │   │   ├── item               # [Item Domain] 상점 및 아이템
│   │   │   │   ├── controller     # StoreController (아이템 구매)
│   │   │   │   ├── domain         # Item, ItemHistory, ItemType (Enum)
│   │   │   │   ├── repository     # ItemRepository, ItemHistoryRepository
│   │   │   │   └── service        # ItemService, ItemPolicy
│   │   │   │
│   │   │   ├── lent               # [Lent Domain] 대여/반납 핵심 로직
│   │   │   │   ├── controller     # LentController (대여, 반납, 아이템 사용)
│   │   │   │   ├── domain         # LentHistory (Entity), ReturnReason
│   │   │   │   ├── repository     # LentRepository
│   │   │   │   └── service        # LentFacadeService (AI 통신 포함), LentService
│   │   │   │
│   │   │   ├── user               # [User Domain] 사용자 및 스케줄러
│   │   │   │   ├── controller     # UserController (내 정보, 출석)
│   │   │   │   ├── domain         # User (Entity), Role (Enum)
│   │   │   │   ├── repository     # UserRepository, AttendanceRepository
│   │   │   │   ├── service        # UserService, UserFacadeService
│   │   │   │   └── scheduler      # LogtimeScheduler, BlackholeScheduler
│   │   │   │
│   │   │   ├── alarm              # [Alarm] 슬랙 알림 시스템
│   │   │   │   ├── controller     # AlarmController
│   │   │   │   ├── domain         # AlarmEvent
│   │   │   │   └── service        # SlackBotService, AlarmEventHandler (Async)
│   │   │   │
│   │   │   ├── global             # [Global] 전역 설정 및 예외 처리
│   │   │   │   ├── aspect         # LoggingAspect (AOP 로깅)
│   │   │   │   ├── config         # SecurityConfig, WebConfig, RedisConfig
│   │   │   │   └── exception      # GlobalExceptionHandler, ErrorCode, ServiceException
│   │   │   │
│   │   │   └── utils              # [Utils] 유틸리티
│   │   │       ├── DateUtil.java
│   │   │       └── FtApiManager.java # 42 Intra API 연동 모듈
│   │   │
│   │   └── resources
│   │       ├── application.yml    # 프로젝트 설정 파일
│   │       ├── logback-spring.xml # 로그 정책 설정
│   │       ├── secret.properties  # [Secret] 민감 정보 (Git 제외)
│   │       └── static             # 정적 리소스
│   │           └── index.html     # 통합 테스트용 웹 페이지
│   │
│   └── test                       # 단위 및 통합 테스트
│       └── java/com/gyeongsan/cabinet
│           ├── LentServiceTest.java
│           └── ...
```
