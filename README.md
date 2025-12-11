📦 42Cabi Gyeongsan (Backend)

42 경산 캠퍼스 사물함 대여/반납 서비스 – REST API 서버

사용자의 편의성과 공정한 사물함 운영을 위해 개발된 백엔드 서비스입니다.
OAuth 로그인, 동시성 제어, 자동화 스케줄러, Slack 알림 등 다양한 기능을 포함합니다.

🛠 Tech Stack
분류	기술
Language	Java 17
Framework	Spring Boot 3.5.8
Database	MariaDB 10.6, Redis
ORM	Spring Data JPA (Hibernate)
Auth	OAuth2 (42 Intra), Spring Security
Infra	Docker, Docker Compose
Tools	Gradle, Slack Webhook
🚀 Key Features
🔒 사물함 대여/반납 프로세스

동시성 제어
MariaDB Pessimistic Lock을 적용해 중복 대여를 원천 차단

아이템 시스템
‘대여권(Item)’을 소비하여 사물함을 대여하는 게임화 요소 도입

검증 로직 강화

블랙홀 예정자(D-3)

중복 대여 방지

고장/사용불가 사물함 체크

⏱ 자동화 스케줄러

블랙홀 처리
매일 자정, 퇴학(Blackhole) 유저의 사물함 자동 반납

연체 관리

반납 기한 초과 사물함 감지

Slack DM으로 자동 알림 전송

⚡ 성능 및 안정성

쿼리 최적화
JPA JOIN FETCH를 사용해 N+1 문제 해결

모니터링
AOP 기반 로깅 시스템으로 요청/응답 시간 기록
에러 발생 시 자동 파일 로깅

보안 강화
DB 비밀번호, API Key 등은 Git에 포함되지 않은 별도 파일로 관리

⚙️ Setup & Run

이 프로젝트는 보안을 위해 secret.properties, .env 파일이 Git에 포함되어 있지 않습니다.
아래 단계에 따라 파일을 생성해주세요.

1. 프로젝트 클론
git clone https://github.com/farmer0010/42_cabinet_backend_mvpmodel.git
cd 42_cabinet_backend_mvpmodel

2. 보안 파일 생성 (필수 ⭐)
A. Docker 환경 변수 파일 (.env)

프로젝트 루트 경로에 .env 파일을 생성한 뒤 아래 내용 입력:

# DB 루트 비밀번호 (원하는 값으로 설정)
DB_ROOT_PASSWORD=your_secure_password

# DB 사용자 (기본값: user)
DB_USER=user

# DB 사용자 비밀번호
DB_PASSWORD=your_secure_password

# 타임존 설정
TZ=Asia/Seoul

B. Spring Boot 비밀 설정 파일 (secret.properties)

src/main/resources/secret.properties 생성 후 입력:

# DB 사용자/비밀번호 (.env 값과 동일해야 함)
spring.datasource.username=root
spring.datasource.password=your_secure_password

# 42 Intra OAuth 인증 키
FT_CLIENT_ID=your_42_client_id
FT_CLIENT_SECRET=your_42_client_secret

# Slack Bot Token
SLACK_BOT_TOKEN=xoxb-your-slack-bot-token

3. 인프라 실행 (MariaDB, Redis)
docker-compose up -d

4. 애플리케이션 실행
./gradlew bootRun


서버는 기본적으로 http://localhost:8080
 에서 실행됩니다.

🧪 API Usage

Base URL

http://localhost:8080

주요 API
기능	Method	URL
로그인	GET	/oauth2/authorization/42
사물함 대여	POST	/v4/lent/cabinets/{cabinetId}
사물함 반납	POST	/v4/lent/return
아이템 구매	POST	/v4/store/buy/{itemId}
📂 Project Structure
├── .github/workflows/
│   └── gradle.yml             # Github Actions CI 설정
├── .env                       # [Secret] Docker 환경 변수
├── .gitignore
├── build.gradle
├── docker-compose.yaml        # DB, Redis 실행 설정
├── gradlew / gradlew.bat
├── settings.gradle
└── src
    ├── main
    │   ├── java/com/gyeongsan/cabinet
    │   │   ├── CabinetApplication.java
    │   │   ├── admin/              # 관리자 기능
    │   │   ├── alarm/              # Slack 봇 서비스
    │   │   ├── auth/               # OAuth2 + Spring Security
    │   │   ├── cabinet/            # 사물함 도메인
    │   │   ├── common/             # 공통 DTO
    │   │   ├── config/             # CORS, Redis 설정
    │   │   ├── global/             # AOP, 전역 예외 처리
    │   │   ├── item/               # 상점/아이템 도메인
    │   │   ├── lent/               # 대여 핵심 로직
    │   │   ├── user/               # 사용자 도메인 + 스케줄러
    │   │   └── utils/              # 42 API 연동
    │   └── resources
    │       ├── application.yml
    │       ├── logback-spring.xml
    │       ├── secret.properties   # [Secret]
    │       └── static/
    └── test/java/com/gyeongsan/cabinet

📬 Contact

문의 또는 개선 제안은 Issue 또는 PR로 자유롭게 남겨주세요!
