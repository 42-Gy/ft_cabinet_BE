# 🗄️ 42Cabi Gyeongsan Ver 4.6 (Safe Mode Edition)

> **42 경산 캠퍼스 사물함 대여/반납 서비스**<br>
> 사용자의 편의성, 공정한 이용, 그리고 **안전한 운영**을 위해 개발된 REST API 서버입니다.

<br>

## 📜 Version History (업데이트 내역)

| 버전 | 주요 변화 | 상세 내용 |
| :--- | :--- | :--- |
| **Ver 1.0** | **MVP 모델** | 기본적인 대여/반납 로직 구현, DB 비관적 락(Lock) 적용 |
| **Ver 2.0** | **보안 & 안정성** | 민감 정보 분리(`.env`), 스케줄러 N+1 문제 해결, 로깅 시스템 구축 |
| **Ver 3.0** | **아키텍처 확장** | **Spring Security + JWT** 도입 (Stateless 전환), 필터 기반 보안 구축 |
| **Ver 4.0** | **게임화 & 상점** | **제곱 패널티($D^2$)**, **아이템 상점** 구현, API 권한 최적화 |
| **Ver 4.5** | **운영 고도화** | 관리자 리팩토링(Intra ID), 반납 메모(Share Code) 기능 |
| **Ver 4.6** | **AI 보류 & 정책 변경** | **AI 청결도 검사 임시 중단 (Safe Mode)**, **패널티 감면 정책 완화(-1일)**, **블랙홀 유저 반납 보류(PENDING)** |

<br>

## 🛠 Tech Stack

| 분류 | 기술 |
| :--- | :--- |
| **Backend** | Java 17, **Spring Boot 3.5.8**, Spring Security, JPA |
| **Database** | MariaDB 10.6, **Redis** (Token/Cache) |
| **Stability** | **Resilience4j** (CircuitBreaker, RateLimiter), **HikariCP** (Connection Pool) |
| **Infra** | Docker, Docker Compose, AWS EC2 |
| **Tools** | Gradle, Slack Webhook, **Spring Actuator**, Swagger UI |
| **AI (Temporarily Disabled)** | Python, FastAPI, OpenCV (현재 모델 정확도 이슈로 기능 비활성화) |

<br>

## 🚀 Key Features (핵심 기능)

### 1. [보류] AI 기반 반납/이사 청결도 검사
> **⚠️ 현재 상태: 비활성화 (Disabled)**<br>
> AI 모델의 청결도 판독 정확도 개선을 위해 해당 기능은 **임시 중단**되었습니다.<br>
> 추후 모델 학습 데이터 확보 및 고도화 후 재가동될 예정입니다.

### 2. 스마트 반납 로직 (Smart Return) - Ver 4.6 [UPDATED]
* **반납 메모(Share Code):** 다음 사용자를 위해 **4자리 비밀번호**를 필수로 입력받습니다.
* **간편 반납:** 사진 촬영 절차 없이, 비밀번호 입력만으로 즉시 반납 및 이사가 가능합니다.

### 3. 블랙홀 유저 정책 변경 (Return Hold Policy) - Ver 4.6 [UPDATED]
* **변경:** 블랙홀(퇴소) 진입 시 사물함이 즉시 '사용 가능'으로 풀리지 않고, **'반납 보류(PENDING)'** 상태로 전환됩니다.
* **목적:** 퇴소자가 짐을 두고 가는 문제를 방지하며, 관리자가 직접 확인 후 상태를 변경할 수 있도록 안전장치를 마련했습니다.

### 4. 패널티 감면 정책 완화
* **변경:** '패널티 감면권' 아이템 사용 시 차감되는 일수가 **기존 2일에서 1일로 조정**되었습니다.

### 5. 고성능/안정성 아키텍처 (Performance & Stability)
* **비동기 처리:** 슬랙 알림 등 부가 작업은 **`@Async`** 스레드로 분리하여 메인 로직의 응답 속도를 보장합니다.
* **API 보호:** 42 Intra API 호출 시 **RateLimiter**가 초당 요청 횟수를 조절하여 IP 차단을 방지합니다.

<br>

## ⚙️ Setup & Run (실행 방법)

### 1. 프로젝트 클론
```bash
git clone [https://github.com/farmer0010/42_cabinet_backend.git](https://github.com/farmer0010/42_cabinet_backend.git)
cd 42_cabinet_backend
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

# AI Server Connection (Disabled)
# ai.server.url=http://localhost:8000/predict
```

### 3. 실행 (Docker Compose)
```bash
# 1. DB & Redis 실행
docker-compose up -d

# 2. 백엔드 서버 실행
./gradlew bootRun
```

<br>

## 🧪 API Usage (Ver 4.6 Updated)

* **Base URL:** `http://localhost:8080`
* **Swagger UI:** `http://localhost:8080/swagger-ui.html`
* **중요 변경 사항:** 반납 및 이사 API는 `application/json` 형식을 사용합니다. (Multipart 제거됨)

### 📦 Cabinet Lent & Return

#### 1. 사물함 반납 (Return)
* **URL:** `POST /v4/lent/return`
* **Content-Type:** `application/json`
* **Body:**
    ```json
    {
      "shareCode": "1234"
    }
    ```

#### 2. 사물함 이사 (Swap)
* **URL:** `POST /v4/lent/swap/{newVisibleNum}`
* **Content-Type:** `application/json`
* **Body:**
    ```json
    {
      "shareCode": "1234"
    }
    ```

### 👤 User & Auth
* **로그인:** `GET /oauth2/authorization/42`
* **내 정보 조회:** `GET /v4/users/me`

### ⚙️ Admin Actions
* **유저 검색 (Intra ID):** `GET /v4/admin/users/{name}`
* **강제 반납:** `POST /v4/admin/cabinets/{visibleNum}/force-return`
    * 실행 시 사물함 상태가 `PENDING`(보류)으로 변경됩니다.

<br>

## 📂 Project Structure

```text
.
├── .github
│   └── workflows
│       └── gradle.yml              # Github Actions CI/CD 설정
├── .env                            # [Secret] DB 및 TimeZone 환경 변수
├── build.gradle                    # 의존성 설정
├── docker-compose.yaml             # MariaDB, Redis 컨테이너 설정
├── src
│   ├── main
│   │   ├── java/com/gyeongsan/cabinet
│   │   │   ├── CabinetApplication.java  # 메인 실행 파일 (@EnableAsync)
│   │   │   │
│   │   │   ├── admin               # [Admin] 관리자 기능
│   │   │   │   ├── controller/AdminController.java
│   │   │   │   ├── dto/AdminUserDetailResponse.java  # 유저 상세 + 사물함 정보
│   │   │   │   ├── dto/CabinetStatusRequest.java
│   │   │   │   ├── dto/CoinProvideRequest.java
│   │   │   │   └── service/AdminService.java         # 강제 반납(PENDING) 로직
│   │   │   │
│   │   │   ├── alarm               # [Alarm] 비동기 알림 시스템
│   │   │   │   ├── dto/AlarmEvent.java
│   │   │   │   ├── AlarmEventHandler.java            # @Async 이벤트 리스너
│   │   │   │   └── SlackBotService.java
│   │   │   │
│   │   │   ├── auth                # [Auth] 인증 및 보안
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
│   │   │   ├── cabinet             # [Cabinet] 사물함 도메인
│   │   │   │   ├── controller/CabinetController.java
│   │   │   │   ├── domain/Cabinet.java
│   │   │   │   ├── domain/CabinetStatus.java         # PENDING 상태 추가됨
│   │   │   │   ├── dto/CabinetDetailResponseDto.java
│   │   │   │   ├── repository/CabinetRepository.java
│   │   │   │   └── service/CabinetService.java
│   │   │   │
│   │   │   ├── global              # [Global] 공통 설정
│   │   │   │   ├── aspect/LoggingAspect.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── RedisConfig.java
│   │   │   │   │   └── WebConfig.java
│   │   │   │   └── exception/
│   │   │   │       ├── ErrorCode.java
│   │   │   │       ├── GlobalExceptionHandler.java
│   │   │   │       └── ServiceException.java
│   │   │   │
│   │   │   ├── item                # [Item] 상점 및 아이템
│   │   │   │   ├── controller/StoreController.java
│   │   │   │   ├── domain/
│   │   │   │   │   ├── Item.java
│   │   │   │   │   └── ItemHistory.java
│   │   │   │   ├── repository/ItemRepository.java
│   │   │   │   └── service/
│   │   │   │       ├── ItemPriceInitializer.java
│   │   │   │       └── StoreService.java
│   │   │   │
│   │   │   ├── lent                # [Lent] 대여/반납 (핵심 로직)
│   │   │   │   ├── controller/LentController.java    # AI 로직 제거, JSON 방식 적용
│   │   │   │   ├── domain/LentHistory.java
│   │   │   │   ├── dto/LentReturnRequest.java        # 반납 DTO (비밀번호 검증)
│   │   │   │   ├── repository/LentRepository.java
│   │   │   │   └── service/LentFacadeService.java    # 트랜잭션 분리 및 정책 적용
│   │   │   │
│   │   │   ├── user                # [User] 사용자 및 스케줄러
│   │   │   │   ├── controller/UserController.java
│   │   │   │   ├── domain/User.java
│   │   │   │   ├── repository/UserRepository.java
│   │   │   │   ├── scheduler/
│   │   │   │   │   ├── BlackholeScheduler.java       # 블랙홀 처리 (PENDING 전환)
│   │   │   │   │   └── LogtimeScheduler.java
│   │   │   │   └── service/UserService.java
│   │   │   │
│   │   │   └── utils               # [Utils] 유틸리티
│   │   │       └── FtApiManager.java                 # 42 API 통신 (@RateLimiter)
│   │   │
│   │   └── resources
│   │       ├── application.yml     # 메인 설정 (Multipart, HikariCP 등)
│   │       ├── logback-spring.xml  # 로깅 설정
│   │       ├── secret.properties   # [Secret] API 키
│   │       └── static/index.html   # 웰컴 페이지
│   │
│   └── test                        # 테스트 코드
│       └── java/com/gyeongsan/cabinet
│           └── CabinetApplicationTests.java
```
