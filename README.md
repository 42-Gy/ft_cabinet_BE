# 🗄️ 42Cabi Gyeongsan Ver 4.5 (Backend)

> **42 경산 캠퍼스 사물함 대여/반납 서비스**<br>
> 사용자의 편의성과 공정한 사물함 이용을 위해 개발된 REST API 서버입니다.

<br>

## 📜 Version History (업데이트 내역)

| 버전 | 주요 변화 | 상세 내용 |
| :--- | :--- | :--- |
| **Ver 1.0** | **MVP 모델** | 기본적인 대여/반납 로직 구현, DB 비관적 락(Lock) 적용 |
| **Ver 2.0** | **보안 & 안정성** | 민감 정보 분리(`.env`), 스케줄러 N+1 문제 해결, 로깅 시스템 구축 |
| **Ver 2.5** | **성능 & 운영** | **비동기 처리(Async)**로 알림 속도 개선, **Actuator** 모니터링, 단위 테스트 도입 |
| **Ver 3.0** | **아키텍처 확장** | **Spring Security + JWT** 도입 (Stateless 전환), 필터 기반 보안 구축 |
| **Ver 3.5** | **보안 & 리워드** | **Refresh Token** 도입, **출석 기반 코인 지급 스케줄러** 구현, 예외 처리 강화 |
| **Ver 4.0** | **게임화 & 상점** | **제곱 패널티($D^2$)**, **아이템 상점(연장/이사/감면)** 구현, API 권한 최적화 |
| **Ver 4.5** | **운영 고도화** | **관리자 API 리팩토링(Name 기반)**, **반납 메모(Share Code) 저장 로직**, **월 50시간 보상** |

<br>

## 🛠 Tech Stack

| 분류 | 기술 |
| :--- | :--- |
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.5.8 |
| **Database** | MariaDB 10.6, **Redis (Token/Cache)** |
| **ORM** | Spring Data JPA (Hibernate) |
| **Auth** | OAuth2 (42 Intra), **Spring Security, JWT** |
| **Infra** | Docker, Docker Compose |
| **Tools** | Gradle, Slack Webhook, **Spring Actuator** |

<br>

## 🚀 Key Features (핵심 기능)

### 1. 상점 및 아이템 시스템 (Shop & Item) - Ver 4.5 [UPDATED] ⭐
* **아이템 상점:** 출석체크로 모은 코인을 사용하여 유용한 아이템을 구매할 수 있습니다.
* **다양한 아이템 구현:**
    * **대여권:** 사물함을 30일간 대여할 수 있는 권한. (**상점 구매 불가**, 월 50시간 학습 보상으로만 획득 가능)
    * **연장권:** 현재 대여 중인 사물함의 만료일을 **15일 연장**합니다. (가격: 1000 코인)
    * **이사권:** 남은 대여 기간을 유지한 채 **다른 사물함으로 이동**합니다. (가격: 100 코인)
    * **감면권:** 누적된 패널티 기간을 **2일 차감**합니다. (가격: 600 코인)

### 2. 관리자 편의성 개선 (Admin Refactor) - Ver 4.5 [NEW] ⭐
* **Intra ID 기반 관리:** 관리자가 `userId(숫자)`가 아닌 **`Intra ID(문자열)`**로 유저를 검색하고 코인을 지급할 수 있도록 개선했습니다.
* **강제 반납 안정화:** 강제 반납 시 사물함 타입(`lentType`)이 소실되는 버그를 수정하여 데이터 무결성을 확보했습니다.
* **유연한 상태 변경:** 사물함의 상태(`AVAILABLE`, `BROKEN` 등)와 타입(`PRIVATE`, `SHARE`)을 자유롭게 변경할 수 있습니다.

### 3. 스마트 반납 로직 (Smart Return) - Ver 4.5 [UPDATED]
* **반납 메모(Share Code):** 사물함 반납 시 사용자가 입력한 비밀번호를 검증하는 대신, **다음 사용자를 위한 메모**로 저장합니다.
* **정보 공유:** 다음 사용자가 사물함을 대여하고 '내 정보'를 조회하면, 이전 사용자가 남긴 비밀번호(메모)를 확인할 수 있습니다.

### 4. 학습 보상 시스템 (Gamification)
* **월간 학습 보상:** 매일 아침 스케줄러가 **42 API**를 호출하여 전날 체류 시간(Logtime)을 누적합니다.
* **대여권 지급:** 매월 1일, 지난달 누적 학습 시간이 **50시간(3,000분)**을 넘긴 유저에게 **대여권(LENT)**을 자동 지급합니다.
* **신규 유저 혜택:** 신규 가입 시 **웰컴 선물**로 대여권 1개를 즉시 지급합니다.

### 5. 보안 및 인증 (Security & Auth)
* **Stateless 인증:** JWT 기반 인증 시스템 구축 및 **Refresh Token**을 통한 자동 갱신 지원.
* **API 권한 최적화:**
    * 사물함 현황 조회(`GET`)는 **Public** 접근 허용.
    * 핵심 로직(`POST`) 및 관리자 기능(`/v4/admin`)은 **`ROLE_ADMIN`** 권한 필수.

<br>

## ⚙️ Setup & Run (실행 방법)

이 프로젝트는 보안을 위해 **환경 설정 파일(`secret.properties`, `.env`)이 Git에 포함되어 있지 않습니다.**

### 1. 프로젝트 클론
```bash
git clone [https://github.com/farmer0010/42_cabinet_backend_mvpmodel.git](https://github.com/farmer0010/42_cabinet_backend_mvpmodel.git)
cd 42_cabinet_backend_mvpmodel
```

### 2. 보안 파일 생성 (필수 ⭐)

#### A. Docker 환경 변수 파일 (`.env`)
프로젝트 **최상단(Root)** 경로에 `.env` 파일을 생성하세요.
```properties
DB_ROOT_PASSWORD=your_secure_password
DB_USER=user
DB_PASSWORD=your_secure_password
TZ=Asia/Seoul
```

#### B. Spring Boot 시크릿 파일 (`secret.properties`)
`src/main/resources/` 경로에 `secret.properties` 파일을 생성하세요.
```properties
spring.datasource.username=root
spring.datasource.password=your_secure_password
spring.security.oauth2.client.registration.42.client-id=your_42_client_id
spring.security.oauth2.client.registration.42.client-secret=your_42_client_secret
SLACK_BOT_TOKEN=xoxb-your-slack-bot-token
jwt.secret=v3_secret_key_42cabi_gyeongsan_must_be_very_long_secret_key
```

### 3. 실행
```bash
docker-compose up -d  # DB, Redis 실행
./gradlew bootRun     # 백엔드 서버 실행
```

<br>

## 🧪 API Usage

* **Base URL:** `http://localhost:8080`

### 👤 User & Auth
* **로그인:** `GET /oauth2/authorization/42`
* **토큰 재발급:** `POST /v4/auth/reissue`
* **내 정보 조회:** `GET /v4/users/me` (누적 학습 시간, 반납 예정일 포함)

### 📦 Cabinet (Public)
* **사물함 현황 조회:** `GET /v4/cabinets/status-summary?floor=2`

### 🛒 Store & Lent
* **아이템 구매:** `POST /v4/store/buy/{itemId}`
* **사물함 대여:** `POST /v4/lent/cabinets/{visibleNum}`
* **사물함 반납:** `POST /v4/lent/return` (Body: `{ "shareCode": "0000" }`)
    * *Tip: `shareCode`는 다음 사용자를 위해 남길 자물쇠 비밀번호입니다.*

### ⚙️ Admin Actions (ROLE_ADMIN Required) [UPDATED]
관리자 기능의 편의성을 위해 **유저 고유 ID(Long) 대신 Intra ID(String)를 사용**하도록 변경되었습니다.

* **대시보드 조회:** `GET /v4/admin/dashboard`
* **유저 상세 검색:** `GET /v4/admin/users/{name}`
* **코인 지급:** `POST /v4/admin/users/{name}/coin`
    * Body: `{ "amount": 500, "reason": "이벤트" }`
* **로그타임 수정:** `PATCH /v4/admin/users/{name}/logtime`
    * Body: `{ "monthlyLogtime": 3000 }`
* **강제 반납:** `POST /v4/admin/cabinets/{visibleNum}/force-return`
* **사물함 상태 변경:** `PATCH /v4/admin/cabinets/{visibleNum}`
    * Body: `{ "status": "AVAILABLE", "lentType": "PRIVATE", "statusNote": null }`

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
│   │   │   │   └── service        # LentFacadeService (트랜잭션 단위), LentService
│   │   │   │
│   │   │   ├── user               # [User Domain] 사용자 및 스케줄러
│   │   │   │   ├── controller     # UserController (내 정보, 출석)
│   │   │   │   ├── domain         # User (Entity), Role (Enum)
│   │   │   │   ├── repository     # UserRepository, AttendanceRepository
│   │   │   │   ├── service        # UserService, UserFacadeService
│   │   │   │   └── scheduler      # LogtimeScheduler (월간 보상 지급)
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
