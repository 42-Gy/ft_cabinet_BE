🗄️ 42Cabi Gyeongsan (Backend)42 경산 캠퍼스 사물함 대여/반납 서비스사용자의 편의성과 공정한 사물함 이용을 위해 개발된 REST API 서버입니다.🛠 Tech Stack분류기술LanguageJava 17FrameworkSpring Boot 3.5.8DatabaseMariaDB 10.6, Redis (Cache/Session)ORMSpring Data JPA (Hibernate)AuthOAuth2 (42 Intra), Spring SecurityInfraDocker, Docker ComposeToolsGradle, Slack Webhook (Notification)🚀 Key Features (핵심 기능)사물함 대여/반납 프로세스동시성 제어: MariaDB의 Pessimistic Lock을 적용하여 중복 대여 문제를 원천 차단했습니다.아이템 시스템: '대여권' 아이템을 소모하여 사물함을 대여하는 게임화 요소를 도입했습니다.검증 로직: 블랙홀 예정자(D-3), 중복 대여, 사물함 상태(고장 등)를 철저히 검증합니다.자동화된 관리 시스템 (Scheduler)블랙홀 처리: 매일 자정, 퇴학(Blackhole) 처리된 유저의 사물함을 자동으로 반납시킵니다.연체 관리: 반납 기한이 지난 사물함을 감지하여 상태를 변경하고, Slack DM으로 경고 알림을 보냅니다.성능 및 안정성쿼리 최적화: JPA JOIN FETCH를 활용하여 N+1 문제를 해결했습니다.모니터링: AOP 기반 로깅 시스템으로 요청/응답 시간을 추적하고, 에러 발생 시 로그 파일로 기록합니다.보안: 민감한 정보(DB 패스워드, API Key)를 환경 변수와 별도 파일로 분리하여 관리합니다.⚙️ Setup & Run (실행 방법)이 프로젝트는 보안을 위해 환경 설정 파일(secret.properties, .env)이 Git에 포함되어 있지 않습니다.실행하려면 아래 단계를 따라 파일을 생성하고, 본인의 환경에 맞는 값을 입력해야 합니다.1. 프로젝트 클론Bashgit clone https://github.com/farmer0010/42_cabinet_backend_mvpmodel.git
cd 42_cabinet_backend_mvpmodel
2. 보안 파일 생성 (필수 ⭐)A. Docker 환경 변수 파일 (.env)프로젝트 최상단(Root) 경로에 .env 파일을 생성하고 아래 형식을 복사하여 값을 채워주세요.Properties# .env (예시)
# DB 루트 비밀번호 (본인이 사용할 비밀번호로 변경하세요)
DB_ROOT_PASSWORD=your_secure_password
# DB 사용자 계정 (기본값: user)
DB_USER=user
# DB 사용자 비밀번호 (본인이 사용할 비밀번호로 변경하세요)
DB_PASSWORD=your_secure_password
# 타임존 설정
TZ=Asia/Seoul
B. Spring Boot 시크릿 파일 (secret.properties)src/main/resources/ 경로에 secret.properties 파일을 생성하고 아래 내용을 입력하세요.(주의: DB 비밀번호는 위 .env 파일에서 설정한 값과 일치해야 합니다)Properties# secret.properties
# DB 접속 정보 (.env의 DB_ROOT_PASSWORD와 일치해야 함)
spring.datasource.username=root
spring.datasource.password=your_secure_password

# 42 API 인증 키 (Intra 42에서 발급받은 키 입력)
FT_CLIENT_ID=your_42_client_id
FT_CLIENT_SECRET=your_42_client_secret

# Slack 봇 토큰 (Slack API에서 발급받은 토큰 입력)
SLACK_BOT_TOKEN=xoxb-your-slack-bot-token
3. 인프라 실행 (Docker)DB와 Redis 컨테이너를 실행합니다.Bashdocker-compose up -d
4. 애플리케이션 실행Bash./gradlew bootRun
서버가 정상적으로 실행되면 http://localhost:8080으로 접속 가능합니다.🧪 API UsageBase URL: http://localhost:8080API 명세:로그인: GET /oauth2/authorization/42대여: POST /v4/lent/cabinets/{cabinetId}반납: POST /v4/lent/return상점: POST /v4/store/buy/{itemId}📂 Project Structure.
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
    │   │   ├── CabinetApplication.java  # 메인 애플리케이션 클래스
    │   │   ├── admin/                   # 관리자 기능
    │   │   │   ├── controller/          # 관리자 API 컨트롤러
    │   │   │   ├── dto/                 # 관리자용 데이터 전송 객체
    │   │   │   └── service/             # 관리자 비즈니스 로직
    │   │   ├── alarm/                   # 알림 서비스
    │   │   │   └── SlackBotService.java # 슬랙 봇 연동
    │   │   ├── auth/                    # 인증 및 인가
    │   │   │   ├── config/              # Security 설정
    │   │   │   └── service/             # OAuth2 유저 서비스
    │   │   ├── cabinet/                 # 사물함 도메인
    │   │   │   ├── domain/              # Entity 및 Enums
    │   │   │   └── repository/          # DB 접근 (비관적 락 적용)
    │   │   ├── common/                  # 공통 모듈
    │   │   │   └── dto/                 # 공통 응답 DTO
    │   │   ├── config/                  # 전역 설정
    │   │   │   ├── RedisConfig.java     # Redis 설정
    │   │   │   └── WebConfig.java       # CORS 설정
    │   │   ├── global/                  # 전역 예외 및 AOP
    │   │   │   ├── aspect/              # 로깅 AOP
    │   │   │   └── exception/           # 전역 예외 처리기
    │   │   ├── item/                    # 아이템/상점 도메인
    │   │   │   ├── controller/          # 상점 API
    │   │   │   ├── domain/              # Entity
    │   │   │   ├── repository/          # DB 접근 (Fetch Join 최적화)
    │   │   │   └── service/             # 상점 비즈니스 로직
    │   │   ├── lent/                    # 대여/반납 도메인 (Core)
    │   │   │   ├── controller/          # 대여 API
    │   │   │   ├── domain/              # Entity
    │   │   │   ├── repository/          # DB 접근
    │   │   │   ├── scheduler/           # 연체 관리 스케줄러
    │   │   │   └── service/             # 대여 핵심 로직 (Facade)
    │   │   ├── user/                    # 사용자 도메인
    │   │   │   ├── domain/              # Entity
    │   │   │   ├── repository/          # DB 접근
    │   │   │   └── scheduler/           # 블랙홀/로그타임 스케줄러
    │   │   └── utils/                   # 유틸리티
    │   │       └── FtApiManager.java    # 42 Intra API 연동
    │   └── resources
    │       ├── application.yml          # 스프링 부트 설정
    │       ├── logback-spring.xml       # 로깅 설정
    │       ├── secret.properties        # [Secret] 애플리케이션 비밀 설정
    │       └── static/                  # 정적 리소스 (테스트 페이지)
    └── test
        └── java/com/gyeongsan/cabinet   # 테스트 코드
