# 🗄️ 42Cabi Gyeongsan Ver 3.0 (Backend)

> **42 경산 캠퍼스 사물함 대여/반납 서비스**<br>
> 사용자의 편의성과 공정한 사물함 이용을 위해 개발된 REST API 서버입니다.

<br>

## 📜 Version History (업데이트 내역)

| 버전 | 주요 변화 | 상세 내용 |
| :--- | :--- | :--- |
| **Ver 1.0** | **MVP 모델** | 기본적인 대여/반납 로직 구현, DB 비관적 락(Lock) 적용 |
| **Ver 2.0** | **보안 & 안정성** | 민감 정보 분리(`.env`), 스케줄러 N+1 문제 해결, 로깅 시스템 구축 |
| **Ver 2.5** | **성능 & 운영** | **비동기 처리(Async)**로 알림 속도 개선, **Actuator** 모니터링, 단위 테스트 도입 |
| **Ver 3.0** | **아키텍처 확장** | **Spring Security + JWT** 도입 (Stateless 전환), **Redis** 연동, 필터 기반 보안 구축 |

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

### 1. 보안 및 인증 (Security & Auth) - Ver 3.0 ⭐
* **Stateless 인증:** 기존 세션(Cookie) 방식을 제거하고 **JWT(JSON Web Token)** 기반 인증 시스템을 구축하여 서버 확장성을 확보했습니다.
* **OAuth2 연동:** 42 Intra 로그인을 통해 사용자 정보를 안전하게 받아오고, 서버 전용 Access Token을 발급합니다.
* **보안 필터 체인:** `JwtAuthenticationFilter`를 커스텀하여 모든 요청의 헤더를 검증하고 유연한 인가 처리를 수행합니다.

### 2. 성능 및 비동기 처리 (Async & Ops) - Ver 2.5 ⭐
* **비동기 이벤트(Event):** 핵심 비즈니스 로직(대여/반납)과 부가 기능(슬랙 알림)을 `Spring Event`로 분리하여 응답 속도를 최적화했습니다.
* **상태 모니터링:** `Spring Actuator`를 도입하여 운영 중인 서버, DB, Redis의 상태(Health Check)를 실시간으로 추적합니다.

### 3. 사물함 비즈니스 로직
* **동시성 제어:** MariaDB의 `Pessimistic Lock`(비관적 락)을 적용하여 중복 대여 문제를 원천 차단했습니다.
* **아이템 시스템:** '대여권' 아이템을 소모하여 사물함을 대여하는 게임화 요소를 도입했습니다.
* **자동화 관리:** 매일 자정 스케줄러가 동작하여 블랙홀 유저 강제 반납 및 연체자 상태 변경을 수행합니다.

### 4. 안정성 및 유지보수
* **쿼리 최적화:** JPA `JOIN FETCH`를 활용하여 연체자 조회 시 발생하는 N+1 문제를 해결했습니다.
* **로깅 시스템:** AOP 기반 로깅으로 요청/응답 시간을 추적하고, 날짜별 로그 파일로 자동 저장합니다.

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
FT_CLIENT_ID=your_42_client_id
FT_CLIENT_SECRET=your_42_client_secret

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
* **API 명세:**
    * **로그인 (토큰 발급):** `GET /oauth2/authorization/42`
    * **대여:** `POST /v4/lent/cabinets/{cabinetId}` (Header: `Authorization: Bearer {token}`)
    * **반납:** `POST /v4/lent/return` (Header: `Authorization: Bearer {token}`)
    * **상점:** `POST /v4/store/buy/{itemId}` (Header: `Authorization: Bearer {token}`)
    * **상태 확인:** `GET /actuator/health`

<br>

## 📂 Project Structure

```text
.
├── .github/workflows/
│   └── gradle.yml       # Github Actions CI 설정 (빌드 자동화)
├── .env                 # [Secret] Docker 환경 변수 (DB 접속 정보 등)
├── .gitignore           # Git 제외 파일 설정
├── build.gradle         # Gradle 의존성 및 플러그인 설정
├── docker-compose.yaml  # Docker 컨테이너 설정 (MariaDB, Redis)
├── gradlew              # Gradle Wrapper 실행 스크립트
├── gradlew.bat          # Gradle Wrapper 실행 스크립트 (Windows)
├── settings.gradle      # 프로젝트 설정
└── src
    ├── main
    │   ├── java/com/gyeongsan/cabinet
    │   │   ├── CabinetApplication.java  # 메인 애플리케이션 (Async 활성화)
    │   │   ├── admin/                   # 관리자 기능 (API, Service, DTO)
    │   │   ├── alarm/                   # 알림 서비스
    │   │   │   ├── AlarmEventHandler.java # [Async] 알림 이벤트 리스너
    │   │   │   ├── SlackBotService.java   # 슬랙 API 호출
    │   │   │   └── dto/AlarmEvent.java    # 알림 이벤트 객체
    │   │   ├── auth/                    # [Ver 3.0] 인증/인가 (JWT Core)
    │   │   │   ├── config/              # Security Config (필터 체인 설정)
    │   │   │   ├── jwt/                 # JWT Provider & Filter
    │   │   │   ├── oauth/               # OAuth Success Handler (토큰 발급)
    │   │   │   └── service/             # OAuth2 유저 서비스
    │   │   ├── cabinet/                 # 사물함 도메인 (Entity, Repository, Lock)
    │   │   ├── common/                  # 공통 모듈 (DTO)
    │   │   ├── config/                  # 전역 설정 (Redis, WebConfig)
    │   │   ├── global/                  # 전역 예외(ExceptionHandler) 및 AOP(Logging)
    │   │   ├── item/                    # 아이템/상점 도메인 (Entity, Repository)
    │   │   ├── lent/                    # 대여/반납 핵심 로직 (Facade, Scheduler)
    │   │   ├── user/                    # 사용자 도메인 (Entity, Repository, Scheduler)
    │   │   └── utils/                   # 유틸리티 (FtApiManager)
    │   └── resources
    │       ├── application.yml          # 스프링 부트 설정
    │       ├── logback-spring.xml       # 로깅 설정 (File Appender)
    │       ├── secret.properties        # [Secret] 애플리케이션 비밀 설정
    │       └── static/                  # 정적 리소스 (JWT 테스트 페이지)
    └── test
        └── java/com/gyeongsan/cabinet   # 테스트 코드 (Unit Test)
```
