# Flyway 베이스라인 (V1) 와 운영 적용 절차

> 이 문서는 **절차 문서**다. 코드 작업 중에는 운영 DB 에 접속하지 않았다. 아래 "운영 적용"은 운영 DB 접근 권한자가 직접, 한 번만 수행한다.

## 1. 구조

| 버전 | 내용 | 새 DB(처음부터) | 운영 DB(이미 스키마 있음) |
|---|---|---|---|
| V1 | 운영 `cabi` 스키마 12개 테이블 (`mysqldump --no-data` 를 정제) | Flyway 가 실행 | **실행되지 않음** — 사람이 `baseline 1` 로 "이미 적용됨" 표시 |
| V2, V3 | `admin_action_log`, `admin_action_log_item`, `undo_of_batch_id` | 실행 | 실행 |
| V4 | `user.ft_grade` 추가 | 실행 | 실행 |
| V5 | `faq`, `faq_question` | 실행 | 실행 |
| V6 | `user.kakao_alarm` 추가, `kakao_notify_consent` 생성(카카오 알림 동의) | 실행 | 실행 |

- `baselineOnMigrate` 는 **계속 false**. 앱이 부팅하면서 스스로 baseline 을 찍는 경로는 없다(설정이 잘못된 DB 를 가리켜도 조용히 마이그레이션이 실행되지 않게 하는 안전장치).
- `FLYWAY_ENABLED` 기본값은 false. 켜는 환경에서만 `true` 로 명시한다.
- 운영은 `ddl-auto: validate` 라 V2~V6 가 만드는 테이블/컬럼이 없으면 새 이미지가 **부팅에 실패**한다. 그래서 아래 순서(baseline → 설정 → 배포)가 중요하다.

## 2. V1 은 어떻게 만들었나

```bash
# 운영 DB 접근 권한자가 받은 스키마 덤프(데이터 없음)
mysqldump --no-data --routines --triggers -h <host> -u <user> -p cabi > baseline.sql

# 정제해서 V1 생성 (원본 baseline.sql 은 레포에 올리지 않는다)
python3 -I scripts/db/sanitize_baseline.py baseline.sql > src/main/resources/db/migration/V1__baseline.sql
```

`mysqldump` 결과를 그대로 V1 로 쓰면 안 되는 이유와 정제 내용:

| 원본 덤프 | 문제 | 정제 |
|---|---|---|
| `DROP TABLE IF EXISTS` (mysqldump 기본값) | V1 이 운영에서 한 번이라도 실행되면 **운영 테이블이 전부 삭제됨** | 제거. V1 에는 `CREATE TABLE` 만 둔다 |
| `/*!40014 SET FOREIGN_KEY_CHECKS=0 */` 등 세션 주석, 호스트·서버 버전 헤더 | 환경 정보, 불필요 | 제거 |
| `AUTO_INCREMENT=103027` 등 | 덤프 시점 카운터(운영 데이터 규모 노출), 새 DB 는 1부터 시작해야 함 | 제거 |
| `COLLATE=utf8mb4_0900_ai_ci` | MySQL 8 전용 이름 → 로컬 compose 의 `mariadb:10.6` 에서 실패 | 제거 (MySQL 8 의 utf8mb4 기본 collation 이 같은 값이라 MySQL 에서는 결과 동일) |
| 알파벳순 테이블 순서 | FK 부모(`user`)보다 자식(`attendance`)이 먼저 나옴 → `FOREIGN_KEY_CHECKS=0` 에 의존해야 함 | 부모 우선으로 재정렬 |
| 트리거/루틴/INSERT/DEFINER | 베이스라인에 있을 이유 없음 | 있으면 정제 **중단** (이번 덤프에는 없었음) |

`idx_cabinet_visible_num` (UNIQUE) 은 운영에서 사람이 수동으로 추가한 인덱스인데, 덤프에 `UNIQUE KEY` 로 들어 있어 V1 에 포함된다.

## 3. 검증한 것 (Testcontainers, 실제 MySQL 8.0 / 8.4 / MariaDB 10.6)

- **충실도**: 원본 덤프를 그대로 실행한 DB 와 V1 을 실행한 DB 를 비교 — 컬럼(타입·NULL·기본값·순서), 인덱스(이름·순서·방향·유니크), FK(이름·규칙), 테이블 엔진/collation 148행이 MySQL 8.0, 8.4 모두 **완전히 동일**(일회성 비교, 레포에 원본 덤프를 두지 않으므로 테스트로는 남기지 않음. 정제본 자체는 아래 가드 테스트가 지킴).
- `FlywayBaselineV1GuardTest`: V1 이 `CREATE TABLE` 만 담는지(DROP/DELETE/INSERT/DEFINER/AUTO_INCREMENT/COLLATE 없음), 12개 테이블, FK 부모 우선, `idx_cabinet_visible_num` 포함.
- `FlywayBaselineMysqlTest`:
  - 새 DB: V1~V6 가 순서대로 적용되고 **16개 전체 엔티티가 `ddl-auto=validate` 통과** (MySQL 8.0, 8.4, MariaDB 10.6)
  - 운영 경로: 기존 스키마 + 데이터에 `baseline(1)` → `migrate` 는 V2~V6 **5개만** 실행, V1 은 실행되지 않아 기존 데이터가 그대로 남음, `validate` 통과, 엔티티 validate 통과
  - 새 DB 경로와 운영 경로의 최종 스키마가 동일
  - baseline 없이 기존 DB 에서 `migrate` 하면 Flyway 가 거부
  - 테스트 서버처럼 `update` 로 만든 스키마(이력 없음)는 `baseline(6)` 로 맞춰야 하고, `baseline(1)` 이면 V2 에서 실패(데이터는 안전)
  - V1 행 없이 V2~V4 만 적용된 이력은 V1 파일이 생기면 검증에서 막힘 (아래 5절)

**한계**: Hibernate `validate` 는 테이블/컬럼 존재와 타입 종류만 본다. 컬럼 길이, 인덱스, FK 는 검사하지 않는다(길이를 바꿔 본 실험으로 확인). 그래서 "스키마 동일성"은 위 충실도 비교와 아래 사전 점검 쿼리가 맡는다.

## 4. 운영 적용 절차 (사람이 수행)

### 사전 준비
1. **백업**: `scripts/backup_db.sh` 또는 `mysqldump --single-transaction --routines --triggers cabi > backup_YYYYMMDD.sql`. 복원 가능한지 확인.
2. 앱 DB 계정 권한: Flyway 가 `flyway_schema_history` 생성 + V2~V6 의 `CREATE TABLE`/`ALTER TABLE` 을 하므로 앱 계정(또는 마이그레이션 전용 계정)에 `CREATE, ALTER, INDEX, REFERENCES` 권한이 있어야 한다(`DROP` 은 필요 없다). 없으면 마이그레이션이 실패한다.
3. Flyway CLI 준비(버전은 앱의 Flyway 와 같은 메이저 권장). 비밀번호는 명령행 인자가 아니라 환경변수로 넘긴다.

### 사전 점검 (읽기 전용 쿼리)
```sql
-- 1) 이력 테이블이 아직 없어야 한다 (있으면 중단하고 상태를 먼저 파악)
SHOW TABLES LIKE 'flyway_schema_history';           -- 빈 결과여야 함
-- 2) V2~V6 가 만드는 것이 아직 없어야 한다
SHOW TABLES LIKE 'admin_action_log%';               -- 빈 결과
SHOW TABLES LIKE 'faq%';                            -- 빈 결과
SHOW COLUMNS FROM `user` LIKE 'ft_grade';           -- 빈 결과
-- 3) 운영 스키마가 V1 과 같은지: 최신 mysqldump --no-data 를 다시 떠서
--    sanitize_baseline.py 로 정제한 결과가 레포의 V1__baseline.sql 과 같아야 한다 (diff 가 비어야 함)
SHOW INDEX FROM cabinet;                            -- idx_cabinet_visible_num (Non_unique=0)
```
3)이 다르면(덤프 이후 스키마가 바뀐 경우) **baseline 을 찍지 말고** V1 을 새 덤프로 다시 만든 뒤 진행한다.

### baseline (1회, 되돌리기 어려우므로 신중히)
```bash
export FLYWAY_PASSWORD='...'    # 셸 히스토리에 남기지 않기
flyway -url="jdbc:mysql://<host>:3306/cabi" -user=<user> \
       -locations=filesystem:src/main/resources/db/migration \
       -baselineVersion=1 -baselineDescription="prod baseline" \
       baseline

flyway -url=... -user=... -locations=filesystem:src/main/resources/db/migration info
# 기대: V1 = Baseline(Success), V2~V6 = Pending
```
- `baseline` 은 `flyway_schema_history` 테이블을 만들고 "1 = 이미 적용됨" 행을 한 줄 넣을 뿐, 다른 테이블을 건드리지 않는다. **`migrate` 는 이 단계에서 실행하지 않는다.**
- `-baselineVersion` 은 반드시 `1`. 6 등으로 찍으면 V2~V6 가 "적용됨"으로 처리돼 실제로는 테이블이 없는데 앱이 부팅 실패한다.

### 배포
1. 운영 App Settings 에 `FLYWAY_ENABLED=true` 설정 (`baselineOnMigrate` 는 설정하지 않음).
2. 새 이미지 배포. 앱이 부팅하면서 V2~V6 를 순서대로 적용한 뒤 JPA `validate` 가 뜬다.
3. 서버가 여러 대여도 Flyway 가 DB 잠금을 잡고 적용하므로 마이그레이션은 한 번만 실행된다.

### 확인
```sql
SELECT installed_rank, version, description, type, success FROM flyway_schema_history ORDER BY installed_rank;
-- 기대: 1 BASELINE / 2,3,4,5,6 SQL 모두 success=1
SHOW TABLES;   -- admin_action_log, admin_action_log_item, faq, faq_question 추가
SHOW COLUMNS FROM `user` LIKE 'ft_grade';
```
앱 로그에 `Successfully applied 5 migrations` 와 부팅 성공, `/actuator/health` 확인.

### 롤백
- V2~V6 는 **추가 전용**(새 테이블, nullable 컬럼, 인덱스)이다. 이전 이미지는 `validate` 에서 추가 컬럼/테이블을 문제 삼지 않아 **이미지만 되돌려도 동작**한다(`ft_grade` 는 NULL 허용).
- 마이그레이션 자체를 지워야 하면 백업에서 복원한다(`flyway undo` 는 사용하지 않음). 복원 후에는 `flyway_schema_history` 도 같이 사라지므로 위 절차를 처음부터 다시 한다.
- 실패한 마이그레이션이 있으면 Flyway 가 해당 행을 `success=0` 으로 남기고 이후 부팅을 막는다. 원인(권한 등)을 고친 뒤 `flyway repair` → 재배포.

## 5. 테스트 서버(`subak-server-test`): `ddl-auto` update → validate

테스트 DB 는 `update` 로 만들어져 있어 **현재 이력 상태에 따라 절차가 다르다**. 먼저 확인:

```sql
SELECT installed_rank, version, type, success FROM flyway_schema_history ORDER BY installed_rank;  -- 테이블이 없을 수도 있음
```

| 상태 | 조치 |
|---|---|
| 이력 테이블 없음 (Flyway 를 켠 적 없음) | 스키마가 이미 V6 까지 반영돼 있으므로 **`-baselineVersion=6`** 로 baseline. (1 로 찍으면 V2 가 "이미 있는 테이블"로 실패하는데 데이터는 안전함) |
| 이력에 1(BASELINE), 2, 3, … 이 있음 | 그대로 OK. V1 은 "baseline 이하"로 무시된다 |
| **이력에 2, 3, 4 만 있고 1 이 없음** | V1 파일이 생겼으므로 `Detected resolved migration not applied to database: 1` 로 **부팅 실패**한다(테스트로 재현·확인). 테스트 DB 는 데이터를 버려도 되므로 `DROP TABLE flyway_schema_history;` 후 위 첫 번째 행처럼 `baseline 6` 를 권장 |

전환:
1. 위 표대로 이력을 정리한다. 테스트 DB 에서 `SHOW INDEX FROM cabinet;` 로 `idx_cabinet_visible_num` 도 확인.
2. App Settings: `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` (또는 항목 삭제 — 기본값이 validate), `FLYWAY_ENABLED=true`.
3. 재시작. 부팅에 실패하면 로그의 `Schema-validation: missing column [...] in table [...]` 가 어디가 다른지 알려 준다. 급하면 `SPRING_JPA_HIBERNATE_DDL_AUTO=update` 로 되돌릴 수 있다.
4. 테스트 서버에서 먼저 한 번 같은 흐름(baseline → 켜기 → 배포)을 리허설한 뒤 운영에 적용하는 것을 권장한다.

엔티티 쪽은 "운영 스키마(V1) + V2~V6" 에 대해 validate 가 통과함을 위에서 확인했으므로, 테스트 DB 가 그 스키마와 같다면 전환으로 깨질 것이 없다. 다만 `update` 로 만든 테스트 DB 는 길이·인덱스가 운영과 다를 수 있고 validate 는 그것을 보지 못하니 주의.

테스트 서버처럼 이미 V1~V5 가 적용돼 있고 `FLYWAY_ENABLED=true` 인 환경은, V6(카카오 알림 동의) 이 들어 있는 이미지를 배포하면 부팅 중에 V6 만 자동으로 적용된다(별도 조치 없음). V6 은 `user` 에 `NOT NULL DEFAULT b'0'` 컬럼을 더하고 새 테이블을 만드는 추가 전용이라, 이전 이미지로 되돌려도 `validate` 는 통과한다.

## 6. 새 환경(데모/로컬 compose)

비어 있는 DB 에 `FLYWAY_ENABLED=true` 로 부팅하면 V1~V6 가 모두 실행된다(MySQL 8.0/8.4, MariaDB 10.6 에서 검증). baseline 은 필요 없다.
주의: V1 에서 collation 을 지정하지 않으므로 MariaDB 는 서버 기본(`utf8mb4_general_ci`), MySQL 8 은 `utf8mb4_0900_ai_ci` 가 된다. 문자 비교 규칙이 미묘하게 달라(악센트·일부 문자 취급) 운영과 같은 동작이 필요한 검증은 MySQL 8 로 하는 것이 맞다.

## 7. V1 을 다시 만들 때
운영 스키마가 바뀐 뒤(예: 수동 인덱스 추가) 새 덤프를 받아 2절 명령을 다시 돌린다. 이미 baseline 을 찍은 운영에서는 V1 내용이 실행되지 않고 Flyway 도 baseline 이하 버전은 검증하지 않으므로, V1 수정은 **새 DB 생성용 정확도**에만 영향을 준다.
