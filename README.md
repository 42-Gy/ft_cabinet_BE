# 🗄️ 42Cabi Gyeongsan Ver 4.8 (AI Enhanced Edition)

> **42 경산 캠퍼스 사물함 대여/반납 서비스**<br>
> 사용자의 편의성, 공정한 이용, 그리고 **지능형 운영**을 위해 개발된 REST API 서버입니다.

<br>

## 📜 Version History (업데이트 내역)

| 버전 | 주요 변화 | 상세 내용 |
| :--- | :--- | :--- |
| **Ver 1.0** | **MVP 모델** | 기본적인 대여/반납 로직 구현, DB 비관적 락(Lock) 적용 |
| **Ver 2.0** | **보안 & 안정성** | 민감 정보 분리(`.env`), 스케줄러 N+1 문제 해결, 로깅 시스템 구축 |
| **Ver 3.0** | **아키텍처 확장** | **Spring Security + JWT** 도입 (Stateless 전환), 필터 기반 보안 구축 |
| **Ver 4.0** | **게임화 & 상점** | **제곱 패널티($D^2$)**, **아이템 상점** 구현, API 권한 최적화 |
| **Ver 4.6** | **Safe Mode** | AI 모델 이슈로 인한 임시 기능 축소 (JSON 반납) 및 정책 완화 |
| **Ver 4.8** | **AI 재가동 & 관리자** | **AI 청결도 검사 재활성화(+Exif 보안)**, **블랙홀 스케줄러 중단**, **관리자 아이템 가격 변경**, **D-3 알림** |

<br>

## 🛠 Tech Stack

| 분류 | 기술 |
| :--- | :--- |
| **Backend** | Java 17, **Spring Boot 3.5.8**, Spring Security, JPA |
| **Database** | MariaDB 10.6, **Redis** (Token/Cache) |
| **Stability** | **Resilience4j** (CircuitBreaker, RateLimiter), **HikariCP** (Connection Pool) |
| **Infra** | Docker, Docker Compose, AWS EC2 |
| **Tools** | Gradle, Slack Webhook, **Spring Actuator** |
| **AI Module** | **WebFlux (WebClient)**, Metadata-extractor (Exif 검증) |

<br>

## 🚀 Key Features (핵심 기능 상세)

### 1. 지능형 AI 반납 시스템 & 보안 (AI-Powered Return)
> **"단순한 반납을 넘어, 다음 사용자를 위한 배려를 시스템화하다."**
* **AI 청결도 검사:** 사용자가 반납 시 업로드한 사물함 내부 사진을 Python(FastAPI) AI 서버로 실시간 전송합니다. 짐이나 쓰레기가 남아있는지 자동으로 분석하여, 깨끗한 상태일 때만 반납을 승인합니다.
* **Exif 메타데이터 보안 (Anti-Replay):** 사진의 촬영 시각 정보를 추출하여 **"현재 시점 기준 10분 이내"**에 촬영된 원본 사진인지 검증합니다. 캡처본이나 과거 사진을 악용한 부정 반납을 원천 차단합니다.

### 2. 수동 승인 및 예외 처리 프로세스 (Manual Approval Flow)
> **"AI가 해결하지 못하는 예외 상황을 관리자가 유연하게 처리합니다."**
* **수동 반납 요청:** AI 검사가 반복적으로 실패하거나 서버 장애 시, 사용자는 사유와 함께 '수동 반납'을 요청할 수 있습니다.
* **PENDING(보류) 상태:** 수동 요청이 접수되면 사물함은 즉시 **`PENDING`** 상태로 잠기며, 다른 사용자가 대여할 수 없게 됩니다.
* **관리자 최종 승인:** 관리자가 현장을 확인한 뒤 대시보드에서 **[승인]** 버튼을 눌러야 비로소 사물함이 `AVAILABLE`(사용 가능) 상태로 전환됩니다.

### 3. 블랙홀(퇴소) 유저 안전 장치
* **기존 문제:** 퇴소자가 발생하면 시스템이 자동으로 반납 처리를 해버려, 짐이 방치되는 문제가 있었습니다.
* **개선된 정책:** 블랙홀 유저는 자동으로 반납되지 않고 관리자 목록에 별도로 집계됩니다. 관리자가 직접 연락하여 짐 수거를 확인한 후, **강제 반납(Force Return)** 기능을 통해 처리합니다.

### 4. 게임화 요소 및 아이템 상점 (Gamification)
* **제곱 패널티($D^2$):** 연체 일수의 제곱만큼 패널티가 부여되는 강력한 제재 정책으로 정시 반납을 유도합니다.
* **코인 & 상점:** 출석체크와 로그타임 보상으로 코인을 획득하고, **[연장권], [패널티 감면권], [이사권]** 등을 구매하여 사용할 수 있습니다.
* **동적 가격 정책:** 관리자가 API를 통해 아이템의 가격을 실시간으로 조정하여 경제 밸런스를 맞출 수 있습니다.

### 5. 고성능/안정성 아키텍처 (Robust Architecture)
* **비동기 처리(Async):** 슬랙 알림, 로그 기록 등 사용자 응답에 영향을 주지 않는 작업은 별도 스레드에서 비동기로 처리합니다.
* **장애 격리(Circuit Breaker):** 42 API나 AI 서버 등 외부 시스템 장애 시, 전체 서비스가 멈추지 않도록 차단기를 작동시킵니다.
* **API 보호(Rate Limiter):** 과도한 API 호출을 제어하여 외부 서비스(42 Intra)의 IP 차단을 방지합니다.

<br>

## ⚙️ Setup & Run (실행 방법)

### 1. 프로젝트 클론
```bash
git clone [https://github.com/farmer0010/42_cabinet_backend.git](https://github.com/farmer0010/42_cabinet_backend.git)
cd 42_cabinet_backend
```

### 2. 환경 변수 설정 (For DevOps) ⚠️
보안을 위해 실제 값은 포함되어 있지 않습니다. 아래 템플릿을 참고하여 설정 파일을 생성하세요.

#### A. `.env` (Project Root)
```properties
# Database Configuration
DB_ROOT_PASSWORD=
DB_USER=
DB_PASSWORD=
TZ=Asia/Seoul
```

#### B. `src/main/resources/secret.properties`
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

# AI Server Connection
ai.server.url=http://localhost:8000
```

### 3. 실행 (Docker Compose)
```bash
# 1. DB & Redis 실행
docker-compose up -d

# 2. 백엔드 서버 실행
./gradlew bootRun
```

<br>

## 🧪 API Usage (Full Specification)

* **Base URL:** `http://localhost:8080`

### 🔐 Auth & User (인증 및 사용자)

| Method | URI | 설명 |
| :--- | :--- | :--- |
| `GET` | `/oauth2/authorization/42` | 42 Intra 로그인 (OAuth2) |
| `GET` | `/v4/users/me` | 내 정보 조회 (대여 정보, 연체 일수, 코인 등) |
| `GET` | `/v4/users/me/lent-histories` | 나의 과거 대여 기록 조회 (페이지네이션) |

### 📦 Cabinet (사물함 조회)

| Method | URI | 설명 |
| :--- | :--- | :--- |
| `GET` | `/v4/cabinets` | 전체 사물함 정보 조회 (건물/층별) |
| `GET` | `/v4/cabinets/{visibleNum}` | 특정 사물함의 상세 정보 조회 |
| `GET` | `/v4/cabinets/simple` | (모바일용) 사물함 현황 단순 조회 |

### 🔑 Lent & Return (대여 및 반납)

#### 1. 사물함 대여 (Start Lent)
* **URL:** `POST /v4/lent/cabinets/{visibleNum}`
* **Description:** 해당 번호의 사물함을 대여합니다. (대여 가능 상태일 경우)

#### 2. 사물함 반납 (Return with AI)
* **URL:** `POST /v4/lent/return`
* **Content-Type:** `multipart/form-data`
* **Parameters:**
  * `file`: 사물함 내부 사진 (필수, Exif 검증)
  * `shareCode`: 다음 사용자를 위한 비밀번호 (4자리)

#### 3. 사물함 이사 (Swap with AI)
* **URL:** `POST /v4/lent/swap/{newVisibleNum}`
* **Content-Type:** `multipart/form-data`
* **Parameters:**
  * `file`: 현재 사물함 내부 사진
  * `shareCode`: 현재 사물함 비밀번호

#### 4. 수동 반납 요청 (Manual Return)
* **URL:** `POST /v4/lent/return/manual`
* **Content-Type:** `application/json`
* **Body:**
    ```json
    {
      "shareCode": "1234",
      "reason": "AI 서버 에러로 인한 요청"
    }
    ```

#### 5. 연장권 사용 (Extension)
* **URL:** `POST /v4/lent/extension`
* **Description:** 인벤토리의 연장권을 사용하여 대여 기간을 늘립니다.

### 🛒 Item Store (상점)

| Method | URI | 설명 |
| :--- | :--- | :--- |
| `GET` | `/v4/store/items` | 상점 아이템 목록 및 가격 조회 |
| `POST` | `/v4/store/items/{itemId}/purchase` | 아이템 구매 (코인 차감) |

### 🛠 Admin Actions (관리자 전용)

| Method | URI | 설명 |
| :--- | :--- | :--- |
| `GET` | `/v4/admin/users/{name}` | 특정 유저 정보 및 대여 현황 조회 |
| `PATCH` | `/v4/admin/users/{name}/logtime` | 유저 로그타임 수동 수정 |
| `POST` | `/v4/admin/users/{name}/coin` | 유저에게 코인 지급 |
| `POST` | `/v4/admin/cabinets/{visibleNum}/force-return` | 강제 반납 (상태 `PENDING` 변경) |
| `PATCH` | `/v4/admin/cabinets/{visibleNum}` | 사물함 상태/메모 수정 |
| `GET` | `/v4/admin/cabinets/pending` | **[NEW]** 수동 승인 대기 목록 조회 |
| `POST` | `/v4/admin/cabinets/{visibleNum}/approve` | **[NEW]** 수동 반납 승인 (잠금 해제) |
| `PATCH` | `/v4/admin/items/{itemName}/price` | **[NEW]** 아이템 가격 변경 |

<br>

## 📂 Project Structure

```text
.
├── .github
│   └── workflows
│       └── gradle.yml              # Github Actions CI/CD 설정
├── .env                            # [Secret] DB 및 TimeZone 환경 변수
├── build.gradle                    # 의존성 설정 (WebFlux, Metadata-extractor 추가됨)
├── docker-compose.yaml             # MariaDB, Redis 컨테이너 설정
├── src
│   ├── main
│   │   ├── java/com/gyeongsan/cabinet
│   │   │   ├── CabinetApplication.java  # 메인 실행 파일 (@EnableAsync)
│   │   │   │
│   │   │   ├── admin               # [Admin] 관리자 기능
│   │   │   │   ├── controller/AdminController.java   # 가격 변경, 수동 승인 API
│   │   │   │   ├── dto/
│   │   │   │   │   ├── AdminUserDetailResponse.java
│   │   │   │   │   ├── CabinetPendingResponseDto.java # [NEW] PENDING 목록용
│   │   │   │   │   ├── CabinetStatusRequest.java
│   │   │   │   │   └── ...
│   │   │   │   └── service/AdminService.java         # 가격 수정, 승인 로직 구현
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
│   │   │   │   ├── domain/CabinetStatus.java         # PENDING 상태 포함
│   │   │   │   ├── dto/CabinetDetailResponseDto.java
│   │   │   │   ├── repository/CabinetRepository.java # findAllByStatus 추가
│   │   │   │   └── service/CabinetService.java
│   │   │   │
│   │   │   ├── global              # [Global] 공통 설정
│   │   │   │   ├── aspect/LoggingAspect.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── RedisConfig.java
│   │   │   │   │   └── WebConfig.java            # WebClient 설정
│   │   │   │   └── exception/
│   │   │   │       ├── ErrorCode.java
│   │   │   │       ├── GlobalExceptionHandler.java
│   │   │   │       └── ServiceException.java
│   │   │   │
│   │   │   ├── item                # [Item] 상점 및 아이템
│   │   │   │   ├── controller/StoreController.java
│   │   │   │   ├── domain/
│   │   │   │   │   ├── Item.java                     # updatePrice 메서드
│   │   │   │   │   └── ItemHistory.java
│   │   │   │   ├── repository/ItemRepository.java    # findByName 추가
│   │   │   │   └── service/
│   │   │   │       ├── ItemPriceInitializer.java
│   │   │   │       └── StoreService.java
│   │   │   │
│   │   │   ├── lent                # [Lent] 대여/반납 (핵심 로직)
│   │   │   │   ├── controller/LentController.java    # AI 복구 (Multipart)
│   │   │   │   ├── domain/LentHistory.java
│   │   │   │   ├── dto/LentReturnRequest.java        # Record 타입
│   │   │   │   ├── repository/LentRepository.java    # D-3 알림 쿼리 추가
│   │   │   │   ├── scheduler/LentScheduler.java      # D-3 알림 스케줄러 추가
│   │   │   │   └── service/
│   │   │   │       ├── ItemCheckService.java         # Exif 검증 & AI 통신
│   │   │   │       └── LentFacadeService.java
│   │   │   │
│   │   │   ├── user                # [User] 사용자 및 스케줄러
│   │   │   │   ├── controller/UserController.java
│   │   │   │   ├── domain/User.java
│   │   │   │   ├── repository/UserRepository.java
│   │   │   │   ├── scheduler/
│   │   │   │   │   ├── BlackholeScheduler.java       # [Disabled] 주석 처리됨
│   │   │   │   │   └── LogtimeScheduler.java         # 로그타임 집계
│   │   │   │   └── service/UserService.java
│   │   │   │
│   │   │   └── utils               # [Utils] 유틸리티
│   │   │       └── FtApiManager.java                 # 42 API 통신 (@RateLimiter)
│   │   │
│   │   └── resources
│   │       ├── application.yml     # 메인 설정
│   │       ├── logback-spring.xml  # 로깅 설정
│   │       ├── secret.properties   # [Secret] API 키
│   │       └── static/index.html   # 웰컴 페이지
│   │
│   └── test                        # 테스트 코드
│       └── java/com/gyeongsan/cabinet
│           └── CabinetApplicationTests.java
```