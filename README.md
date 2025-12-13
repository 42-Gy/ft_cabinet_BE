# 🗄️ 42Cabi Gyeongsan Ver 4.0 (Backend)

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

### 1. 상점 및 아이템 시스템 (Shop & Item) - Ver 4.0 [NEW] ⭐
* **아이템 상점:** 사용자는 출석 보상으로 얻은 코인을 사용하여 유용한 아이템을 구매할 수 있습니다.
* **다양한 아이템 구현:**
    * **대여권:** 사물함을 30일간 대여할 수 있는 권한.
    * **연장권:** 현재 대여 중인 사물함의 만료일을 **15일 연장**합니다.
    * **이사권:** 남은 대여 기간을 유지한 채 **다른 사물함으로 이동**합니다.
    * **감면권:** 누적된 패널티 기간을 **2일 차감**합니다.

### 2. 고도화된 패널티 시스템 (Penalty Logic) - Ver 4.0 [NEW] ⭐
* **제곱 패널티 ($D^2$):** 공정한 사물함 회전을 위해, 연체 시 연체일의 제곱만큼 패널티가 부과됩니다. (예: 3일 연체 시 9일간 대여 불가)
* **대여 차단:** `penaltyDays > 0`인 사용자는 시스템 레벨에서 대여 및 상점 이용이 제한됩니다.

### 3. 보안 및 인증 (Security & Auth)
* **Stateless 인증:** 기존 세션(Cookie) 방식을 제거하고 **JWT(JSON Web Token)** 기반 인증 시스템을 구축하여 서버 확장성을 확보했습니다.
* **Refresh Token 시스템:** Access Token 만료 시, **Redis**에 저장된 Refresh Token을 통해 자동으로 재발급받아 로그인 유지를 돕습니다.
* **API 권한 최적화:** * 사물함 현황 조회(`GET`)는 **인증 없이(Public)** 접근 가능하도록 개방했습니다.
    * 대여/반납/구매(`POST`) 및 **관리자 기능($/v4/admin$)**은 철저한 인증 및 **`ROLE_ADMIN` 인가**를 요구합니다.

### 4. 코인 지급 시스템 (Gamification)
* **출석 연동 코인 지급:** 매일 아침 스케줄러가 **42 API**를 호출하여 전날 체류 시간(Logtime)을 조회하고, 시간에 비례하여 유저에게 **코인을 자동 지급**합니다.
* **KST/UTC 타임존 처리:** 42 API(UTC)와 한국 시간(KST) 간의 시차를 고려하여 정확한 일일 출석 시간을 계산합니다.

### 5. 성능 및 비동기 처리 (Async & Ops)
* **비동기 이벤트(Event):** 핵심 비즈니스 로직(대여/반납)과 부가 기능(슬랙 알림)을 `Spring Event`로 분리하여 응답 속도를 최적화했습니다.
* **상태 모니터링:** `Spring Actuator`를 도입하여 운영 중인 서버, DB, Redis의 상태(Health Check)를 실시간으로 추적합니다.

### 6. 사물함 비즈니스 로직 (Core)
* **동시성 제어:** MariaDB의 `Pessimistic Lock`(비관적 락)을 적용하여 중복 대여 문제를 원천 차단했습니다.
* **자동화 관리:** 매일 자정 스케줄러가 동작하여 블랙홀 유저 강제 반납 및 연체자 상태 변경을 수행합니다.
* **안정성:** JPA `JOIN FETCH`를 활용하여 연체자 조회 시 발생하는 N+1 문제를 해결했습니다.

<br>

## ⚙️ Setup & Run (실행 방법)

이 프로젝트는 보안을 위해 **환경 설정 파일(`secret.properties`, `.env`)이 Git에 포함되어 있지 않습니다.**
실행하려면 아래 단계를 따라 파일을 생성하고, **본인의 환경에 맞는 값을 입력**해야 합니다.

### 1. 프로젝트 클론
```bash
git clone [https://github.com/farmer0010/42_cabinet_backend_mvpmodel.git](https://github.com/farmer0010/42_cabinet_backend_mvpmodel.git)
cd 42_cabinet_backend_mvpmodel
```

### 2. 보안 파일 생성 (필수 ⭐)

#### A. Docker 환경 변수 파일 (`.env`)
프로젝트 **최상단(Root)** 경로에 `.env` 파일을 생성하고 아래 형식을 복사하여 값을 채워주세요.

```properties
# .env (예시)
# DB 루트 비밀번호 (본인이 사용할 비밀번호로 변경하세요)
DB_ROOT_PASSWORD=your_secure_password
# DB 사용자 계정 (기본값: user)
DB_USER=user
# DB 사용자 비밀번호 (본인이 사용할 비밀번호로 변경하세요)
DB_PASSWORD=your_secure_password
# 타임존 설정
TZ=Asia/Seoul
```

#### B. Spring Boot 시크릿 파일 (`secret.properties`)
`src/main/resources/` 경로에 `secret.properties` 파일을 생성하고 아래 내용을 입력하세요.
*(주의: DB 비밀번호는 위 .env 파일에서 설정한 값과 일치해야 합니다)*

```properties
# secret.properties
# DB 접속 정보 (.env의 DB_ROOT_PASSWORD와 일치해야 함)
spring.datasource.username=root
spring.datasource.password=your_secure_password

# 42 API 인증 키 (Intra 42에서 발급받은 키 입력)
spring.security.oauth2.client.registration.42.client-id=your_42_client_id
spring.security.oauth2.client.registration.42.client-secret=your_42_client_secret

# Slack 봇 토큰 (Slack API에서 발급받은 토큰 입력)
SLACK_BOT_TOKEN=xoxb-your-slack-bot-token

# JWT 비밀키 (32자 이상 임의의 문자열 입력)
jwt.secret=v3_secret_key_42cabi_gyeongsan_must_be_very_long_secret_key
```

### 3. 인프라 실행 (Docker)
DB와 Redis 컨테이너를 실행합니다.
```bash
docker-compose up -d
```

### 4. 애플리케이션 실행
```bash
./gradlew bootRun
```
* 서버가 정상적으로 실행되면 `http://localhost:8080`으로 접속 가능합니다.

<br>

## 🧪 API Usage

* **Base URL:** `http://localhost:8080`

### 👤 User & Auth
* **로그인 (토큰 발급):** `GET /oauth2/authorization/42`
* **토큰 재발급:** `POST /v4/auth/reissue` (Cookie: `refresh_token`)
* **내 정보 조회:** `GET /v4/users/me` (패널티, 아이템 정보 포함)
* **출석 체크:** `POST /v4/users/attendance`

### 📦 Cabinet (Public)
* **사물함 현황 조회:** `GET /v4/cabinets/status-summary?floor=2` (로그인 불필요)

### 🛒 Store & Lent (Auth Required)
* **아이템 구매:** `POST /v4/store/buy/{itemId}`
    * `1`: 대여권, `2`: 연장권, `3`: 이사권, `4`: 감면권
* **사물함 대여:** `POST /v4/lent/cabinets/{cabinetId}`
* **사물함 반납:** `POST /v4/lent/return`

### ✨ Item Actions (Ver 4.0)
* **연장권 사용:** `POST /v4/lent/extension`
* **이사권 사용:** `POST /v4/lent/swap/{newCabinetId}`
* **감면권 사용:** `POST /v4/lent/penalty-exemption`

### ⚙️ Admin Actions (ROLE_ADMIN Required) [NEW]
* **대시보드 조회:** `GET /v4/admin/dashboard`
* **유저 상세 검색:** `GET /v4/admin/users/{name}`
* **코인 지급:** `POST /v4/admin/users/{userId}/coin`
* **강제 반납:** `POST /v4/admin/cabinets/{cabinetId}/force-return`
* **사물함 상태 변경:** `PATCH /v4/admin/cabinets/{cabinetId}`

<br>

## 📂 Project Structure

```text
.
├── .github/workflows/
│   └── gradle.yml       # Github Actions CI 설정 (빌드 자동화)
├── .env                 # [Secret] Docker 환경 변수 (DB 접속 정보 등)
├── .gitignore           # Git 제외 파일 설정
├── build.gradle         # Gradle 의존성 및 플러그인 설정
├── docker-compose.yaml  # Docker 컨테이너 설정 (MariaDB, Redis)
├── gradlew              # Gradle Wrapper 실행 스크립트
├── gradlew.bat          # Gradle Wrapper 실행 스크립트 (Windows)
├── settings.gradle      # 프로젝트 설정
└── src
    ├── main
    │   ├── java/com/gyeongsan/cabinet
    │   │   ├── CabinetApplication.java  # 메인 애플리케이션
    │   │   ├── admin/                   # [NEW] 관리자 기능 (API, Service, DTO)
    │   │   ├── alarm/                   # 알림 서비스 (Slack)
    │   │   │   ├── AlarmEventHandler.java # [Async] 알림 이벤트 리스너
    │   │   │   └── SlackBotService.java   # 슬랙 API 호출
    │   │   ├── auth/                    # 인증/인가 (JWT & Security)
    │   │   │   ├── config/              # Security Config (필터, 핸들러 설정)
    │   │   │   ├── jwt/                 # JWT Provider & Filter
    │   │   │   └── oauth/               # OAuth Success Handler
    │   │   ├── cabinet/                 # 사물함 도메인 (Entity, Lock)
    │   │   ├── common/                  # 공통 모듈 (DTO)
    │   │   ├── config/                  # 전역 설정 (Redis, WebConfig)
    │   │   ├── global/                  # 전역 예외 및 로깅
    │   │   ├── item/                    # [Ver 4.0] 아이템/상점 도메인
    │   │   ├── lent/                    # 대여/반납 핵심 로직 (Facade)
    │   │   ├── user/                    # 사용자 도메인 (패널티, 스케줄러)
    │   │   │   └── scheduler/           # LogtimeScheduler (코인 지급)
    │   │   └── utils/                   # 유틸리티 (FtApiManager)
    │   └── resources
    │       ├── application.yml          # 스프링 부트 설정
    │       ├── logback-spring.xml       # 로깅 설정
    │       ├── secret.properties        # [Secret] 비밀 설정
    │       └── static/                  # 정적 리소스 (테스트용)
    └── test
        └── java/com/gyeongsan/cabinet   # 테스트 코드
```
