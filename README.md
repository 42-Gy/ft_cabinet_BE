
# 🗄️ 42Cabi Gyeongsan Ver 1.1

> **42 경산 캠퍼스 지능형 사물함 대여/반납 서비스**<br>
> 사용자의 편의성, 공정한 이용, 게임화(Gamification), 그리고 **시스템의 안정성**을 모두 갖춘 REST API 서버입니다.

<br>

## 🏗️ System Architecture (시스템 아키텍처)

> **Dockerized Infra & Monitoring System**<br>
> Nginx 리버스 프록시와 Prometheus/Grafana 모니터링 시스템이 구축되었습니다.

```mermaid
graph TD
    %% 클라이언트 및 진입점
    Client(["User Client<br>Web/Mobile"]) -->|HTTP / Port 80| Nginx["🦁 Nginx Web Server<br>Reverse Proxy"]
    
    %% 백엔드 영역
    subgraph "Backend Container"
        Nginx -->|"Proxy Pass<br>Port 8080"| SpringBoot["☕ Core API Server<br>Spring Boot 3.5"]
        Security["Spring Security<br>JWT Filter"]
        Scheduler["Schedulers<br>Lent/Logtime"]
    end

    %% 모니터링 영역 (New)
    subgraph "Monitoring System"
        Prometheus["🔥 Prometheus<br>Metric Collector"]
        Grafana["📊 Grafana<br>Visualization"]
        
        SpringBoot -.->|"/actuator/prometheus"| Prometheus
        Prometheus -->|"Data Source"| Grafana
    end

    %% 데이터 영역
    subgraph "Data Persistence"
        MariaDB[("🐬 MariaDB 10.6<br>Main DB")]
        Redis[("🔴 Redis<br>Token/Cache")]
    end

    %% 외부 서비스
    subgraph "External Services"
        AI_Server["🤖 AI Server<br>Python FastAPI"]
        Intra_API["42 Intra API<br>OAuth2"]
        Kakao_API["💬 Kakao API<br>OAuth2"]
        Google_API["🔍 Google API<br>OAuth2"]
        Slack["Slack Bot<br>Web API"]
        Azure_Blob["☁️ Azure Blob<br>Image Storage"]
    end

    %% 연결 관계
    SpringBoot -->|Read/Write| MariaDB
    SpringBoot -->|Cache/Session| Redis
    SpringBoot -->|"WebClient<br>Async Request"| AI_Server
    AI_Server -->|"Analysis Result"| SpringBoot
    SpringBoot -->|"OAuth2 Auth"| Intra_API
    SpringBoot -->|"OAuth2 Link/Auth"| Kakao_API
    SpringBoot -->|"OAuth2 Link/Auth"| Google_API
    SpringBoot -->|API Call| Slack
    SpringBoot -->|"Image Upload"| Azure_Blob
```

<br>

## 🗺️ User Flow (서비스 이용 흐름도)

> 사용자가 로그인부터 반납, 상점 이용까지 경험하는 주요 프로세스입니다.

```mermaid
flowchart TD
    %% 노드 스타일 정의
    classDef start fill:#f9f,stroke:#333,stroke-width:2px,color:black;
    classDef process fill:#e1f5fe,stroke:#0277bd,stroke-width:2px,color:black;
    classDef decision fill:#fff9c4,stroke:#fbc02d,stroke-width:2px,color:black;
    classDef endNode fill:#eeeeee,stroke:#333,stroke-width:2px,color:black;

    Start((Start)):::start --> Login["🔐 42 Intra 로그인"]:::process
    Login --> Main["🏠 메인 페이지 / 대시보드"]:::process

    %% 메인 페이지에서의 분기
    Main --> Action_Lent{"사물함 대여?"}:::decision
    Main --> Action_My{"내 정보 관리?"}:::decision
    Main --> Action_Store{"상점 이용?"}:::decision
    Main --> Action_Attend{"출석 체크?"}:::decision
    Main --> Action_Calendar{"일정 확인?"}:::decision

    %% 1. 대여 프로세스
    Action_Lent -- Yes --> Select["📦 사물함 선택"]:::process
    Select --> Check_Lent{"대여 가능?"}:::decision
    Check_Lent -- "No (Full/Ban)" --> Main
    Check_Lent -- Yes --> Rent_Success["🔑 대여 완료"]:::process
    Rent_Success --> Main

    %% 2. 내 정보 & 반납 프로세스
    Action_My -- Yes --> MyPage["👤 마이 페이지"]:::process
    MyPage --> Return_Btn{"반납 하기?"}:::decision
    Return_Btn -- Yes --> Upload["📸 인증 사진 업로드"]:::process
    Upload --> AI_Check{"AI 청결도 검사"}:::decision
    AI_Check -- Fail --> Manual["수동 반납 요청 (사유 입력)"]:::process
    AI_Check -- Pass --> Return_Success["✅ 반납 완료"]:::process
    Manual --> Main
    Return_Success --> Main

    %% 3. 상점 프로세스
    Action_Store -- Yes --> Store["🏪 아이템 상점"]:::process
    Store --> Buy{"아이템 구매?"}:::decision
    Buy -- "연장권" --> Use_Ext["⏳ 기간 연장"]:::process
    Buy -- "이사권" --> Use_Swap["🚚 사물함 이동"]:::process
    Use_Ext --> Main
    Use_Swap --> Main

    %% 4. 출석 및 캘린더 프로세스
    Action_Attend -- Click --> Reward["💰 코인 획득"]:::process
    Action_Calendar -- Click --> Calendar["📅 캘린더 페이지"]:::process
    Reward --> Main
    Calendar --> Main

    %% 종료
    Main --> Logout{"로그아웃?"}:::decision
    Logout -- Yes --> End((End)):::endNode
```

<br>

## 📂 Project Structure (상세 프로젝트 구조)

> **Core Architecture:** Hexagonal Architecture<br>
> 도메인 로직이 외부 인프라(DB, Redis, AI, Slack 등)에 의존하지 않도록 **Port 인터페이스**로 추상화하고,<br>
> 각 인프라 기술을 **Adapter**로 분리하여 테스트 용이성과 교체 가능성을 확보했습니다.

```text
.
├── .github/workflows/gradle.yml        # Github Actions CI/CD 파이프라인
├── .env                                # [Secret] DB, TimeZone, Root Password
├── build.gradle                        # 의존성: WebFlux, Actuator, Resilience4j
├── docker-compose.yaml                 # [Infra] Full Stack Orchestration
├── nginx/conf.d/default.conf           # [Infra] Nginx Reverse Proxy Config
├── prometheus/prometheus.yml           # [Infra] Monitoring Config
├── src
│   ├── main
│   │   ├── java/com/gyeongsan/cabinet
│   │   │   ├── CabinetApplication.java
│   │   │   │
│   │   │   ├── domain                  # ═══ 도메인 계층 (핵심 비즈니스) ═══
│   │   │   │   ├── cabinet
│   │   │   │   │   ├── port/in/CabinetQueryUseCase.java      # Inbound Port
│   │   │   │   │   ├── port/out/CabinetRepositoryPort.java   # Outbound Port
│   │   │   │   │   └── service/CabinetDomainService.java     # 도메인 서비스
│   │   │   │   ├── user
│   │   │   │   │   ├── port/in/UserUseCase.java
│   │   │   │   │   ├── port/out/UserRepositoryPort.java
│   │   │   │   │   ├── port/out/AttendanceRepositoryPort.java
│   │   │   │   │   ├── port/out/BannedUserRepositoryPort.java
│   │   │   │   │   └── service/UserDomainService.java
│   │   │   │   ├── lent
│   │   │   │   │   ├── port/in/LentUseCase.java
│   │   │   │   │   ├── port/out/LentRepositoryPort.java
│   │   │   │   │   ├── port/out/ReservationPort.java         # Redis 추상화
│   │   │   │   │   ├── port/out/ImageUploadPort.java         # Azure 추상화
│   │   │   │   │   ├── port/out/AiCheckPort.java             # AI 서버 추상화
│   │   │   │   │   └── port/out/FtApiPort.java               # 42 API 추상화
│   │   │   │   ├── item
│   │   │   │   │   ├── port/in/StoreUseCase.java
│   │   │   │   │   ├── port/out/ItemRepositoryPort.java
│   │   │   │   │   ├── port/out/ItemHistoryRepositoryPort.java
│   │   │   │   │   └── service/StoreDomainService.java
│   │   │   │   ├── coin
│   │   │   │   │   └── port/out/CoinHistoryRepositoryPort.java
│   │   │   │   ├── calendar
│   │   │   │   │   ├── port/in/CalendarUseCase.java
│   │   │   │   │   ├── port/out/CalendarEventRepositoryPort.java
│   │   │   │   │   └── service/CalendarDomainService.java
│   │   │   │   ├── alarm
│   │   │   │   │   └── port/out/AlarmPort.java                # 알림 추상화
│   │   │   │   └── auth
│   │   │   │       ├── port/in/LinkAccountUseCase.java        # Inbound Port
│   │   │   │       ├── port/out/OauthLinkRepositoryPort.java  # Outbound Port
│   │   │   │       ├── port/out/OAuthApiClientPort.java       # Outbound Port
│   │   │   │       └── service/OauthLinkService.java          # 도메인 서비스
│   │   │   │
│   │   │   ├── application             # ═══ 애플리케이션 계층 (유스케이스 조합) ═══
│   │   │   │   └── lent
│   │   │   │       └── LentApplicationService.java   # 대여 프로세스 오케스트레이션
│   │   │   │
│   │   │   ├── adapter                 # ═══ 어댑터 계층 (인프라 구현체) ═══
│   │   │   │   ├── in/web              # --- Inbound Adapter (Controller) ---
│   │   │   │   │   ├── CabinetController.java
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   ├── LentController.java
│   │   │   │   │   ├── StoreController.java
│   │   │   │   │   ├── CalendarEventController.java
│   │   │   │   │   ├── AdminController.java
│   │   │   │   │   └── auth/AuthController.java
│   │   │   │   ├── out/persistence     # --- Outbound Adapter (DB) ---
│   │   │   │   │   ├── cabinet/CabinetPersistenceAdapter.java
│   │   │   │   │   ├── user/UserPersistenceAdapter.java
│   │   │   │   │   ├── user/AttendancePersistenceAdapter.java
│   │   │   │   │   ├── user/BannedUserPersistenceAdapter.java
│   │   │   │   │   ├── lent/LentPersistenceAdapter.java
│   │   │   │   │   ├── item/ItemPersistenceAdapter.java
│   │   │   │   │   ├── item/ItemHistoryPersistenceAdapter.java
│   │   │   │   │   ├── coin/CoinHistoryPersistenceAdapter.java
│   │   │   │   │   ├── calendar/CalendarEventPersistenceAdapter.java
│   │   │   │   │   └── auth/OauthLinkPersistenceAdapter.java
│   │   │   │   ├── out/external        # --- Outbound Adapter (외부 서비스) ---
│   │   │   │   │   ├── ai/AiServerAdapter.java       # AI 서버 통신
│   │   │   │   │   ├── azure/AzureBlobAdapter.java    # Azure 이미지 업로드
│   │   │   │   │   ├── slack/SlackAlarmAdapter.java    # Slack DM 전송
│   │   │   │   │   ├── ft/FtApiAdapter.java            # 42 API 통신
│   │   │   │   │   ├── kakao/KakaoOAuthApiClientAdapter.java
│   │   │   │   │   └── google/GoogleOAuthApiClientAdapter.java
│   │   │   │   └── out/cache           # --- Outbound Adapter (캐시) ---
│   │   │   │       └── redis/ReservationRedisAdapter.java  # 사물함 예약
│   │   │   │
│   │   │   ├── admin                   # [Admin] 관리자 (기존 구조 유지)
│   │   │   │   ├── controller/AdminController.java
│   │   │   │   ├── dto/
│   │   │   │   └── service/AdminService.java          # Port 인터페이스 의존
│   │   │   │
│   │   │   ├── alarm                   # [Alarm] 비동기 알림 이벤트
│   │   │   │   ├── dto/AlarmEvent.java
│   │   │   │   ├── AlarmEventHandler.java
│   │   │   │   └── SlackBotService.java
│   │   │   │
│   │   │   ├── auth                    # [Auth] 스프링 시큐리티 및 JWT 필터링 설정
│   │   │   │   ├── config/SecurityConfig.java
│   │   │   │   ├── domain/UserPrincipal.java
│   │   │   │   ├── jwt/JwtTokenProvider.java
│   │   │   │   └── service/CustomOAuth2UserService.java
│   │   │   │
│   │   │   ├── cabinet/                # [Entity] 사물함 엔티티 & JPA Repository
│   │   │   ├── user/                   # [Entity] 유저 엔티티 & Scheduler
│   │   │   ├── lent/                   # [Entity] 대여 기록 엔티티
│   │   │   ├── item/                   # [Entity] 아이템 엔티티
│   │   │   ├── coin/                   # [Entity] 코인 이력 엔티티
│   │   │   ├── calendar/               # [Entity] 캘린더 엔티티
│   │   │   ├── global/                 # [Global] 전역 설정, 예외 처리
│   │   │   └── utils/FtApiManager.java # 42 API 통신 모듈
│   │   │
│   │   └── resources
│   │       ├── application.yml
│   │       ├── logback-spring.xml
│   │       ├── secret.properties       # [Secret] Git 제외
│   │       └── static/index.html
│   │
│   └── test
│       └── java/com/gyeongsan/cabinet/CabinetApplicationTests.java
```

### 🔀 의존성 방향 (Dependency Rule)

```mermaid
graph LR
    subgraph "Adapter 계층"
        WEB["🌐 Web Adapter<br>(Controller)"]
        DB["🗄️ Persistence Adapter<br>(JPA)"]
        EXT["🔌 External Adapter<br>(AI, Slack, Azure, 42API)"]
        CACHE["🔴 Cache Adapter<br>(Redis)"]
    end

    subgraph "Application 계층"
        APP["⚙️ Application Service<br>(UseCase 조합)"]
    end

    subgraph "Domain 계층"
        PORT_IN["📥 Inbound Port<br>(UseCase Interface)"]
        DOMAIN["💎 Domain Service<br>(비즈니스 로직)"]
        PORT_OUT["📤 Outbound Port<br>(SPI Interface)"]
    end

    WEB -->|의존| PORT_IN
    PORT_IN -.->|구현| DOMAIN
    PORT_IN -.->|구현| APP
    DOMAIN -->|의존| PORT_OUT
    APP -->|의존| PORT_OUT
    DB -.->|구현| PORT_OUT
    EXT -.->|구현| PORT_OUT
    CACHE -.->|구현| PORT_OUT
```

<br>

## 📊 Database Schema (ERD)

> **Entity Relationship Diagram**<br>
> 프로젝트의 데이터베이스 구조와 엔티티 간의 상관관계를 나타냅니다.

```mermaid
erDiagram
    %% -------------------------------------------------------------------------------------
    %% 관계 (Relationships) - 한글화
    %% -------------------------------------------------------------------------------------
    
    USER ||--o{ ATTENDANCE : "출석체크 함"
    USER ||--o{ LENT_HISTORY : "대여 기록 보유"
    USER ||--o{ ITEM_HISTORY : "아이템 구매/사용 이력"
    USER ||--o{ COIN_HISTORY : "코인 거래 이력"
    USER ||--o{ CALENDAR_EVENT : "일정 등록"
    USER ||--o{ OAUTH_LINK : "소셜 연동 정보 보유"
    
    CABINET ||--o{ LENT_HISTORY : "대여 이력 포함"
    
    ITEM ||--o{ ITEM_HISTORY : "아이템 정보 참조"

    %% -------------------------------------------------------------------------------------
    %% 엔티티 정의 (Entity Definitions)
    %% -------------------------------------------------------------------------------------

    USER {
        Long id PK
        Long version "낙관적 락 버전"
        String name "유니크 - 인트라 ID"
        String email "유니크 - 이메일"
        String role "권한 - USER ADMIN MASTER"
        Long coin "보유 코인"
        Integer penaltyDays "패널티 일수"
        Integer monthlyLogtime "월간 접속 시간"
        LocalDateTime blackholedAt "블랙홀 날짜"
        LocalDateTime deletedAt "탈퇴 날짜"
        boolean slackAlarm "슬랙 알림 여부"
        boolean emailAlarm "이메일 알림 여부"
        boolean pushAlarm "푸시 알림 여부"
    }

    ATTENDANCE {
        Long id PK
        Long user_id FK "유저 ID"
        LocalDate attendanceDate "출석 날짜"
    }

    LENT_HISTORY {
        Long id PK
        Long user_id FK "유저 ID"
        Long cabinet_id FK "사물함 ID"
        LocalDateTime startedAt "대여 시작일"
        LocalDateTime expiredAt "대여 만료일"
        LocalDateTime endedAt "반납일 - null이면 대여중"
        String returnMemo "반납 시 메모"
        boolean isAutoExtension "자동 연장 설정"
        String photoUrl "반납 사진 URL"
    }

    CABINET {
        Long id PK
        Integer visibleNum "사물함 번호"
        String status "상태 - AVAILABLE FULL BROKEN PENDING DISABLED"
        String lentType "타입 - PRIVATE SHARE"
        Integer maxUser "최대 수용 인원"
        String statusNote "상태 비고"
        Integer floor "층"
        String section "구역"
        Integer row "그리드 행 위치"
        Integer col "그리드 열 위치"
    }

    ITEM {
        Long id PK
        String name "아이템 이름"
        String type "타입 - EXTENSION SWAP LENT EXEMPTION"
        Long price "가격"
        String description "설명"
    }

    ITEM_HISTORY {
        Long id PK
        Long user_id FK "유저 ID"
        Long item_id FK "아이템 ID"
        LocalDateTime purchaseAt "구매 일시"
        LocalDateTime usedAt "사용 일시 - null이면 미사용"
    }

    COIN_HISTORY {
        Long id PK
        Long user_id FK "유저 ID"
        Long amount "거래량 - 양수 지급 음수 사용"
        String type "ATTENDANCE WATERMELON ITEM_PURCHASE ADMIN_GRANT ADMIN_REVOKE"
        String description "상세 사유"
        LocalDateTime createdAt "거래 발생 시각"
    }

    CALENDAR_EVENT {
        Long id PK
        String title "일정 제목"
        String description "상세 설명"
        LocalDate eventDate "일정 날짜"
        LocalDateTime createdAt "생성일"
        Long announcer_id FK "작성자 User"
    }

    BANNED_USER {
        Long id PK
        String intraId "차단된 유저 인트라 ID"
        String reason "차단 사유"
        LocalDateTime bannedAt "차단 일시"
    }

    OAUTH_LINK {
        Long id PK
        Long user_id FK "유저 ID"
        String provider "소셜 공급사 - kakao, google"
        String providerId "소셜 고유 ID"
        String providerEmail "소셜 이메일"
        LocalDateTime linkedAt "연동 일시"
    }
```

<br>

## 📜 Version History (개발 연혁)

| 버전 | 주요 변화 | 상세 내용 |
| :--- | :--- | :--- |
| **Ver 0.1** | **MVP** | 핵심 대여/반납 로직 구현, DB 비관적 락(Pessimistic Lock) 적용 |
| **Ver 0.2** | **Security** | 민감 정보 분리(`.env`), 스케줄러 N+1 문제 해결, 로깅 시스템 구축 |
| **Ver 0.3** | **Auth** | **Spring Security + JWT** 도입 (Stateless 전환), 42 OAuth2 연동 |
| **Ver 0.4** | **Gamification** | **패널티($D*3$)**, **아이템 상점(이사/연장/감면)** 구현 |
| **Ver 0.5** | **AI & Admin** | **AI 청결도 검사**, **Exif 보안**, 관리자 수동 승인 프로세스, 블랙홀 유저 보호 |
| **Ver 0.6** | **Infra & DevOps** | **Docker Compose**, **Nginx**(Reverse Proxy), **Prometheus & Grafana**(Monitoring) 도입 |
| **Ver 0.7** | **Stability & UX** | **반납/이사 사유 입력**, **코인 동시성 제어(낙관적 락)** 보안 패치 |
| **Ver 0.8** | **Auto-Extension** | **자동 연장 시스템**, **스케줄러 고도화(D-7/D-1 알림)**, 관리자 모니터링 API 추가 |
| **Ver 0.9** | **Logic Refinement** | **블랙홀 유예(D+7)**, **스케줄러 최적화(시간분산)**, **Intra ID 알림**, 블랙홀 대여제한 해제 |
| **Ver 1.0** | **Production Release** | **인앱 카메라 전용 모드**, **코인 거래 추적 시스템**, **캘린더 일정 관리**, **블랙리스트 관리 API**, Rate Limiting, 하드코딩 값 외부화 등 프로덕션 안정화 완료 |
| **Ver 1.1** | **Hexagonal Architecture** | 레이어드 → **헥사고날(Ports & Adapters)** 아키텍처 전환. **18개 Port 인터페이스**, **14개 Adapter**, **5개 Domain Service** 구축. 도메인 로직의 인프라 독립성 확보 및 테스트 용이성 강화. API 계약 변경 없음 |
| **Ver 1.2** | **Social Login & Extension** | 카카오 및 구글 소셜 로그인 연동 모듈 추가. 헥사고날(Ports & Adapters) 아키텍처에 부합하도록 인증 및 연동 구조 리팩토링 및 다형성(Strategy Pattern) 적용. 연동용 API 엔드포인트 공통화 (`/v4/auth/link/{provider}`) 및 예외 복구 흐름 개선 |

<br>

## 🛠 Tech Stack

| 분류 | 기술 |
| :--- | :--- |
| **Backend** | Java 17, **Spring Boot 3.5.8**, Spring Security, Spring Data JPA |
| **Database** | MariaDB 10.6, **Redis** (Token Storage & Caching) |
| **Infra** | **Docker Compose**, AWS EC2, **Nginx** (Reverse Proxy) |
| **Monitoring** | **Prometheus** (Metrics), **Grafana** (Visualization), **Actuator** |
| **Stability** | **Graceful Shutdown**, **DB Indexing**, **Resilience4j**, **Logback (Rolling)** |
| **Tools** | Gradle, **Slack Bot (Web API)**, **Spring Actuator** |
| **AI Module** | **WebFlux (WebClient)**, Python FastAPI (Image Analysis) |

<br>

## 🚀 Key Features (상세 기능 설명)

### 1. 🏗️ 탄탄한 인프라 및 모니터링 (Infrastructure & Monitoring)
* **Nginx Reverse Proxy:** 80 포트로 유입되는 트래픽을 관리하며, 실제 유저 IP(`X-Forwarded-For`)를 백엔드로 안전하게 전달합니다.
* **Full Dockerization:** 백엔드, DB, Redis, Nginx, 모니터링 툴까지 `docker-compose`로 한 번에 오케스트레이션합니다.
* **Prometheus & Grafana:** JVM 메모리, CPU 사용량, DB 커넥션 풀 상태를 실시간 시각화하여 장애를 사전에 감지합니다.

### 2. 🤖 개선된 AI 반납 시스템 (AI-Powered Return)
* **AI 청결도 검사:** 반납 시 업로드한 사물함 내부 사진을 Python(FastAPI) AI 서버로 실시간 전송. 쓰레기나 짐 방치 여부를 분석하여 자동 승인/거절 처리.
* **인앱 카메라 검증 (In-App Camera):** 갤러리 업로드를 차단하고 **앱 내 카메라로만 촬영**하도록 강제하여, 과거 사진이나 캡처본을 이용한 어뷰징을 원천 차단했습니다. (Exif 메타데이터 의존성 제거)
* **수동 반납 (사유 입력):** AI 검사 실패 시, 사용자가 직접 **사유를 입력하고 강제 반납**을 요청할 수 있습니다. 사물함은 `PENDING` 상태가 되며 관리자가 해당 사유를 확인 후 승인합니다.

### 3. 🍉 수동 출석 & 황금 수박 이벤트 (New in v5.0)
* **수동 출석:** 기존 자동 집계 방식을 폐지하고, 유저가 홈페이지의 **[출석하기]** 버튼을 직접 눌러야 코인을 획득하도록 변경 (유저 리텐션 강화).
* **보상 체계:**
    * **Daily:** 매일 1회 **100 코인** 지급.
    * **Golden Watermelon:** 매월 **20회차** 출석 달성 시 **2,000 코인** 보너스 지급.

### 4. 🛡️ 시스템 안정성 및 성능 (Robustness & Performance)
* **동시성 제어(Concurrency):** `User` 엔티티에 **낙관적 락(`@Version`)**을 적용하여 코인 중복 사용(Double Spending)을 원천 차단했습니다.
* **Graceful Shutdown:** 배포나 서버 재시작 시, 진행 중인 대여/반납 요청을 강제로 끊지 않고 **안전하게 완료한 뒤 종료**되도록 설정하여 데이터 유실을 방지합니다.
* **DB 인덱싱(Indexing):** 대여 기록(`LentHistory`)의 핵심 컬럼(`user_id`, `cabinet_id`, `ended_at`)에 인덱스를 적용하여, 데이터가 수십만 건 쌓여도 **조회 속도가 저하되지 않도록 최적화**했습니다.
* **Timezone 동기화:** Docker 컨테이너 레벨에서 `Asia/Seoul` 타임존을 강제하여, 서버 환경에 상관없이 **출석 체크와 연체료 계산**이 정확한 시간에 수행됩니다.
* **WebClient Timeout:** AI 서버 통신 시 3초 타임아웃을 강제 적용하여 외부 장애 전파를 차단합니다.
* **Logback Rolling Policy:** 로그 파일 용량(10MB/3GB) 제한으로 디스크 장애 예방.

### 5. 🎮 게임화 및 상점 (Gamification)
* **패널티($D*3$):** 연체 시 `연체일수 * 3` 만큼 대여 불가 기간을 부여하여 정시 반납 유도.
* **아이템 상점:** 출석과 로그타임으로 모은 코인을 사용하여 아이템 구매.
    * **🚚 이사권 (Swap):** 반납 절차 없이 즉시 다른 빈 사물함으로 이동.
    * **⏳ 연장권 (Extension):** 현재 대여 중인 사물함 기간을 15일 연장. (최대 **5회** 연장 가능)
    * **🛡️ 감면권 (Exemption):** 연체 패널티 기간 1일 감면.
    * **⏳ 자동 연장 (Auto-Extension):** (New) 유저가 **자동 연장 설정(`ON`)**을 하고 연장권을 보유 중이라면, 대여 만료 1일 전(`D-1`) 시스템이 자동으로 아이템을 사용하여 연장합니다.

### 6. 👑 관리자 기능 (Admin Dashboard)
* **블랙홀 유저 보호:** 퇴소자 발생 시 자동 반납되지 않고 별도 목록으로 관리, 관리자가 짐 수거 확인 후 **강제 반납**.
* **경제 밸런스 조절:** 상점의 아이템 가격을 API로 실시간 변경 가능.
* **유저/사물함 관리:** 코인 수동 지급, 사물함 고장/복구 처리, 강제 반납, 로그타임 수정 등.

### 7. 📅 캘린더 및 일정 관리 (New)
* **일정 등록:** 관리자가 반납 마감일, 서버 점검 등 주요 일정을 등록하여 공지할 수 있습니다.
* **월별 조회:** 사용자는 달력을 통해 월별 주요 이벤트를 한눈에 확인할 수 있습니다.

### 8. 👮‍♂️ 관리자 감사 기능 강화 (Admin Audit)
* **전체 유저 조회:** 페이징을 지원하는 전체 유저 목록 조회 API로 회원 관리 효율성을 높였습니다.
* **반납 사진 감사:** 정상 처리된 반납 건에 대해서도 사진을 조회할 수 있어, 불시 점검 및 사물함 상태 모니터링이 가능합니다.

### 9. ⚡ 이사 전용 실시간 예약 (Swap Reservation) [New]
* **15분 선점(Ticketing):** 사용자가 이사하고 싶은 사물함을 발견하면, **이사 전용 예약**을 통해 **15분간** 해당 사물함을 선점할 수 있습니다.
* **Redis TTL:** Redis를 활용한 만료 시간 관리로, 예약 후 15분 내에 이사를 완료하지 않으면 예약이 자동 취소되어 다른 사용자가 이용 가능해집니다.

<br>

## 🔄 System Logic & Sequence Diagrams

> 주요 비즈니스 로직의 상세 흐름입니다.

### 1. 사물함 대여 (동시성 제어 적용)
```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 사용자
    participant Controller as 🎮 LentController
    participant UseCase as 📥 LentUseCase
    participant Service as ⚙️ LentApplicationService
    participant DB as 🗄️ Database

    User->>Controller: "대여하기" 클릭 (POST /lent)
    activate Controller
    Controller->>UseCase: startLent()
    activate Service
    
    Note right of DB: "🔒 비관적 락 (Pessimistic Lock)<br/>동시 요청 방지"
    Service->>DB: "SELECT ... FOR UPDATE"
    
    alt 🚫 이미 대여중 (FULL)
        Service-->>Controller: 예외 발생 (LENT_FULL)
        Controller-->>User: "400 Error (이미 대여된 사물함입니다.)"
    else ✅ 대여 가능
        Service->>DB: LentHistory 생성
        Service->>DB: 사물함 상태 변경 (FULL)
        Service-->>Controller: 대여 성공
        Controller-->>User: "200 OK (대여 완료!)"
    end
    deactivate Service
    deactivate Controller
```

### 2. AI 스마트 반납 (Smart Return)
```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 사용자
    participant Controller as 🎮 LentController
    participant Service as ⚙️ LentApplicationService
    participant AI as 🤖 AI Server (Python)
    participant Azure as ☁️ Azure Blob

    User->>Controller: "반납 사진 전송 (POST /return)"
    activate Controller
    Controller->>Service: 반납 요청 위임
    activate Service
    
    Service->>AI: 📡 이미지 청결도 분석 요청
    activate AI
    AI-->>Service: "✅ 분석 결과 (CLEAN / DIRTY)"
    deactivate AI

    alt ❌ 더러움 (AI 실패)
        Service-->>Controller: 반납 거절
        Controller-->>User: 400 Bad Request
        Note over User, Controller: "💡 계속 실패 시 '수동 반납(사유 입력)' 요청 가능"
    else ✅ 깨끗함
        Service->>Azure: 📸 사진 업로드
        activate Azure
        Azure-->>Service: (URL 획득)
        deactivate Azure

        Service->>DB: 사물함 상태 변경 (AVAILABLE)
        Service-->>Controller: 반납 성공
        Controller-->>User: "200 OK (반납 완료!)"
    end
    deactivate Service
    deactivate Controller
```

### 3. 아이템 구매 및 제한 (Item Purchase)
```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 사용자
    participant Service as ⚙️ StoreDomainService
    participant DB as 🗄️ Database

    User->>Service: "연장권 구매 요청 (buyItem)"
    activate Service
    
    Service->>DB: "🔍 1. 현재 보유 개수 확인 (Inventory Check)"
    Service->>DB: "🔍 2. 이번 달 구매 횟수 확인 (Monthly Check)"
    
    alt 🚫 제한 초과 (보유 2개 or 월 2회)
        Service-->>User: "예외 발생 (LIMIT_EXCEEDED)"
    else ✅ 구매 가능
        Note right of DB: "🔒 낙관적 락 (@Version)<br/>중복 구매(Double Spending) 방지"
        Service->>DB: "💰 코인 차감 & 아이템 지급"
        Service-->>User: 구매 성공
    end
    deactivate Service
```

### 4. 이사권 사용 (Transaction Swap)
```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 사용자
    participant Service as ⚙️ LentApplicationService
    participant AI as 🤖 AI Server
    participant Azure as ☁️ Azure Blob
    participant DB as 🗄️ Database

    %% 0. 이사 예약 (선점)
    User->>Service: "이사 예약 요청 (forSwap=true)"
    activate Service
    Service->>Service: Redis Key 저장 (TTL 15min)
    Service-->>User: "200 OK (예약 완료)"
    deactivate Service

    User->>Service: "이사 요청 (사진 포함)"
    activate Service
    
    %% 1. AI 검사
    Service->>AI: 📡 청결도 분석 요청
    activate AI
    AI-->>Service: "✅ CLEAN"
    deactivate AI

    %% 2. 이미지 업로드
    Service->>Azure: 📸 사진 업로드
    activate Azure
    Azure-->>Service: (URL 획득)
    deactivate Azure

    %% 3. 트랜잭션
    rect rgb(240, 248, 255)
        Note over Service, DB: 🔄 Atomic Transaction (Service)
        Service->>DB: "1. 이사권 차감"
        Service->>DB: "2. 기존 반납 처리 (URL 저장)"
        Service->>DB: "3. 새 대여 생성"
    end

    alt 🚫 실패 시 (AI/DB Error)
        Service->>DB: Rollback
        Service-->>User: 에러 응답
    else ✅ 성공 시 (Commit)
        Service->>DB: Commit
        Service-->>User: "200 OK (이사 완료)"
    end
    deactivate Service
```

### 5. 캘린더 일정 관리 (Calendar Management)
```mermaid
sequenceDiagram
    autonumber
    actor Admin as 👮‍♂️ 관리자
    actor User as 👤 사용자
    participant Controller as 🎮 CalendarController
    participant Service as ⚙️ CalendarDomainService
    participant DB as 🗄️ Database

    %% 관리자 일정 등록
    Admin->>Controller: "일정 등록 (POST /admin/calendar)"
    activate Controller
    Controller->>Service: createEvent()
    activate Service
    Service->>DB: "일정 저장 (INSERT)"
    Service-->>Controller: 등록 성공
    Controller-->>Admin: "200 OK (일정 추가됨)"
    deactivate Service
    deactivate Controller

    %% 사용자 일정 조회
    User->>Controller: "달력 조회 (GET /calendar)"
    activate Controller
    Controller->>Service: getEvents(month)
    activate Service
    Service->>DB: "일정 목록 조회 (SELECT)"
    Service-->>Controller: "이벤트 리스트 반환"
    Controller-->>User: "200 OK (달력 데이터)"
    deactivate Service
    deactivate Controller
```

<br>

## 🧪 API Specification (전체 API 목록)

### 1. 🔐 인증 (Auth)
| Method | URI | 설명 |
| :--- | :--- | :--- |
| `GET` | `/oauth2/authorization/42` | 42 Intra 로그인 (OAuth2) |
| `POST` | `/v4/auth/reissue` | Access Token 재발급 |
| `POST` | `/v4/auth/logout` | 로그아웃 (Refresh Token 삭제) |

### 2. 👤 유저 (User)
| Method | URI | 설명 |
| :--- | :--- | :--- |
| `GET` | `/v4/users/me` | 내 정보 (대여, 연체, 코인, **[NEW] 재화/아이템 사용 이력 포함**) 조회 |
| `GET` | `/v4/users/me/lent-histories` | 나의 과거 대여 기록 조회 |
| `POST` | `/v4/users/attendance` | **[NEW]** 수동 출석 체크 (코인 획득) |
| `GET` | `/v4/users/attendance` | 이번 달 출석 현황 조회 |

### 3. 📦 사물함 조회 (Cabinet)
| Method | URI | 설명 |
| :--- | :--- | :--- |
| `GET` | `/v4/cabinets` | 건물/층별 사물함 배치도 및 상태 조회 |
| `GET` | `/v4/cabinets/status-summary` | 층별 잔여 좌석 요약 정보 |
| `GET` | `/v4/cabinets/{cabinetId}` | 사물함 상세 정보 (공유 사물함 인원 등) |

### 4. 🔑 대여 및 반납 (Lent)
| Method | URI | 설명 |
| :--- | :--- | :--- |
| `POST` | `/v4/lent/cabinets/{visibleNum}` | 사물함 대여 시작 |
| `POST` | `/v4/lent/reservation/{visibleNum}` | **[NEW]** 사물함 예약 (15분 선점, 대여 중이면 이사 예약으로 자동 처리) |
| `POST` | `/v4/lent/return` | **[AI/Manual]** 반납 (forceReturn=true 시 강제 반납/사유 입력) |
| `POST` | `/v4/lent/swap/{newVisibleNum}` | **[Item]** 이사권을 사용해 사물함 이동 |
| `POST` | `/v4/lent/extension` | **[Item]** 연장권을 사용해 기간 연장 |
| `POST` | `/v4/lent/renew` | **[Ticket]** 대여권을 새로 사용하여 기간 연장 (31일) |
| `PATCH` | `/v4/lent/extension/auto` | **[NEW]** 자동 연장 설정 ON/OFF 토글 |
| `POST` | `/v4/lent/penalty-exemption` | **[Item]** 패널티 감면권 사용 |

### 5. 🏪 상점 (Store)
| Method | URI | 설명 |
| :--- | :--- | :--- |
| `GET` | `/v4/store/items` | 구매 가능한 아이템 목록 및 가격 조회 |
| `POST` | `/v4/store/buy/{itemId}` | 아이템 구매 (코인 차감) |

> **구매 API Error Codes:**
> * `EXTENSION_ITEM_LIMIT_EXCEEDED`: 연장권은 최대 **5개**까지만 보유 가능.
> * `EXTENSION_ITEM_PURCHASE_LIMIT_EXCEEDED`: 연장권은 매월 최대 **5회**만 구매 가능.

### 6. 📅 캘린더 (Calendar) [New]
| Method | URI | 설명 |
| :--- | :--- | :--- |
| `GET` | `/v4/calendar/events` | 월별 일정 목록 조회 |

### 7. 🛡️ 관리자 (Admin)
| Method | URI | 설명 |
| :--- | :--- | :--- |
| `GET` | `/v4/admin/dashboard` | 전체 통계 대시보드 |
| `GET` | `/v4/admin/users` | **[NEW]** 전체 유저 목록 조회 (페이징) |
| `GET` | `/v4/admin/users/{name}` | 특정 유저 정보 및 대여 이력 검색 |
| `POST` | `/v4/admin/users/{name}/coin` | 유저에게 코인 수동 지급 |
| `PATCH` | `/v4/admin/users/{name}/logtime` | 유저 로그타임 수동 수정 |
| `POST` | `/v4/admin/users/{name}/penalty` | 유저에게 패널티 수동 부여 |
| `DELETE` | `/v4/admin/users/{name}/penalty` | 유저 패널티 해제 (감면) |
| `POST` | `/v4/admin/users/{name}/items` | 유저에게 아이템 수동 지급 |
| `PATCH` | `/v4/admin/cabinets/{visibleNum}` | 사물함 상태(고장 등) 변경 |
| `POST` | `/v4/admin/cabinets/{visibleNum}/force-return` | 관리자 권한 강제 반납 |
| `GET` | `/v4/admin/cabinets/pending` | 수동 반납 승인 대기 목록 조회 |
| `GET` | `/v4/admin/returns/photos` | **[NEW]** 반납 완료된 사물함 사진 조회 (Audit) |
| `POST` | `/v4/admin/cabinets/{visibleNum}/approve` | 수동 반납 최종 승인 (잠금 해제) |
| `PATCH` | `/v4/admin/items/{itemName}/price` | 상점 아이템 가격 변경 |
| `POST` | `/v4/admin/alarm/emergency` | 전체 유저 긴급 공지(DM) 발송 |
| `GET` | `/v4/admin/cabinets/overdue` | 현재 연체 중인 유저 목록 조회 |
| `GET` | `/v4/admin/cabinets/{visibleNum}` | 사물함 상세 정보 조회 |
| `GET` | `/v4/admin/stats/coins` | 주간 코인 흐름 통계 (지급/사용) |
| `GET` | `/v4/admin/stats/items` | 아이템 사용 통계 + 출석/수박씨 집계 |
| `POST` | `/v4/admin/calendar/events` | **[NEW]** 일정 등록 |
| `PUT` | `/v4/admin/calendar/events/{id}` | **[NEW]** 일정 수정 |
| `DELETE` | `/v4/admin/calendar/events/{id}` | **[NEW]** 일정 삭제 |
| `GET` | `/v4/admin/banned-users` | **[NEW]** 블랙리스트 유저 목록 조회 |
| `POST` | `/v4/admin/banned-users` | **[NEW]** 블랙리스트 유저 등록 (intraId로 차단) |
| `DELETE` | `/v4/admin/banned-users/{intraId}` | **[NEW]** 블랙리스트 유저 해제 |

> **블랙리스트 API:**
> * **차단 방식:** `intraId` (42 로그인 이름)로 차단. 차단된 유저가 로그인 시도 시 OAuth 에러 발생.
> * 요청 예시 (등록): `{ "intraId": "username", "reason": "차단 사유" }`

<br>

## ⚙️ Setup & Run

### 1. 환경 설정 (Configuration)
보안을 위해 실제 설정 파일은 저장소에 포함되지 않습니다. 아래 파일을 생성하여 환경 변수를 설정하세요.

**A. `.env` 파일 생성 (Root Directory)**  
프로젝트 루트 디렉토리에 `.env` 파일을 생성하고 아래 내용을 작성하세요.
```properties
# Database
DB_ROOT_PASSWORD=root_password
DB_USER=cabi
DB_PASSWORD=cabi_password

# Redis
REDIS_PORT=6379

# OAuth & Security
FT_CLIENT_ID=your_42_client_id
FT_CLIENT_SECRET=your_42_client_secret
JWT_SECRET=your_jwt_strong_secret_key
SLACK_BOT_TOKEN=xoxb-your-slack-bot-token

# Service URLs
FRONTEND_URL=http://localhost
AI_SERVER_URL=http://ai_server:8000
COOKIE_SECURE=false
CORS_ALLOWED_ORIGINS=http://localhost,http://localhost:3000

# Timezone
TZ=Asia/Seoul
```

**B. `src/main/resources/secret.properties` (Optional)**
`.env`로 대체 가능하나, 로컬 실행 시 필요할 수 있습니다. `application.yml`의 환경 변수를 대체할 수 있도록 동일한 키를 포함해야 합니다.
```properties
# Database
spring.datasource.username=cabi
spring.datasource.password=cabi_password

# Security & OAuth
jwt.secret=your_jwt_strong_secret_key
FT_CLIENT_ID=your_42_client_id
FT_CLIENT_SECRET=your_42_client_secret
SLACK_BOT_TOKEN=xoxb-your-slack-bot-token

# Service Config
FRONTEND_URL=http://localhost
AI_SERVER_URL=http://localhost:8000
CORS_ALLOWED_ORIGINS=http://localhost,http://localhost:3000
```

### 2. 실행 (Docker Compose)
모든 서비스(Nginx, Backend, DB, Monitoring)를 한 번에 실행합니다.

```bash
# 1. 애플리케이션 빌드
./gradlew clean build -x test

# 2. 전체 인프라 실행 (Background)
docker-compose up -d --build
```

### 3. 접속 정보
* **메인 서비스:** `http://localhost` (Port 80)
* **Grafana:** `http://localhost:3000` (계정: admin / admin)
* **Prometheus:** `http://localhost:9090`

### 4. 테스트 계정 정보 (Test Accounts)
`data.sql`을 통해 초기 데이터가 로드됩니다. 관리자 권한이 필요한 경우 DB에서 직접 `role`을 `ADMIN`으로 변경하거나 초기 데이터를 확인하세요.

> **Tip:** 로그인 후 `/v4/admin/users/{your_intra_id}/coin` API를 통해 코인을 추가로 지급받아 상점 기능을 테스트해볼 수 있습니다.

### 5. 주요 테스트 시나리오
1. **대여/반납:** 메인 화면에서 사물함 선택 -> 대여 -> 내 정보 -> 반납 (사진 업로드)
2. **자동 연장:** 상점에서 `연장권` 구매 -> `/v4/lent/extension/auto` API로 자동 연장 ON 설청 -> (DB에서 만료일 조작하여 테스트 close)
3. **관리자 모드:** URL에 `/admin/login` 접근 -> (Admin 계정 필요) -> 대시보드 확인
