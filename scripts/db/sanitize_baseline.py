#!/usr/bin/env python3
"""운영 DB 의 `mysqldump --no-data` 결과를 Flyway V1 베이스라인으로 정제한다.

사용법:
    python3 scripts/db/sanitize_baseline.py baseline.sql > src/main/resources/db/migration/V1__baseline.sql

정제 이유 (원본 덤프를 그대로 V1 으로 쓰면 안 되는 이유):
  1. DROP TABLE IF EXISTS — mysqldump 기본값(--add-drop-table). V1 이 운영에서 한 번이라도 실행되면
     운영 테이블이 전부 삭제된다. V1 에는 CREATE TABLE 만 남긴다.
  2. 세션/버전 주석(/*!...*/, /*M!...*/), 호스트·서버 버전 헤더 — 환경 정보이고 실행에도 불필요.
  3. AUTO_INCREMENT=N — 덤프 시점의 카운터 값(운영 데이터 규모 정보). 새 DB 는 1부터 시작해야 한다.
  4. COLLATE=utf8mb4_0900_ai_ci — MySQL 8 전용 이름이라 MariaDB(로컬 compose 는 mariadb:10.6)에서 실패.
     MySQL 8 의 utf8mb4 기본 collation 이 같은 값이라 생략해도 결과가 같다.
  5. 테이블 순서 — 덤프는 알파벳순이라 FK 부모(user 등)보다 자식(attendance)이 먼저 나온다.
     FOREIGN_KEY_CHECKS=0 에 의존하지 않도록 부모가 먼저 오게 위상 정렬한다.

덤프에 트리거·루틴·INSERT·DEFINER 가 있으면 의도치 않은 내용이므로 정제를 중단한다.
"""
import re
import sys

FORBIDDEN = [
    (re.compile(r"\bCREATE\s+(?:DEFINER\s*=\s*\S+\s+)?(?:TRIGGER|PROCEDURE|FUNCTION|VIEW|EVENT)\b", re.I), "트리거/루틴/뷰/이벤트"),
    (re.compile(r"\bDEFINER\s*=", re.I), "DEFINER"),
    (re.compile(r"^\s*INSERT\s+INTO\b", re.I | re.M), "INSERT(데이터)"),
]
SKIP_TABLES = {"flyway_schema_history"}  # Flyway 자신의 이력 테이블은 베이스라인에 포함하지 않는다.

HEADER = """\
-- V1: 운영 DB(cabi) 스키마 베이스라인.
--
-- 운영 DB 의 `mysqldump --no-data` 를 scripts/db/sanitize_baseline.py 로 정제한 것이다.
-- 이 파일은 '새 DB 를 처음부터 만들 때'만 실행된다. 운영 DB 는 사람이 한 번
-- `flyway baseline -baselineVersion=1` 로 이 버전을 '이미 적용됨'으로 표시하므로 실행되지 않는다.
-- (baselineOnMigrate 는 사용하지 않는다. docs/db/FLYWAY_BASELINE.md 참고)
--
-- 주의: 이 파일에는 CREATE TABLE 만 둔다. DROP/DELETE/TRUNCATE/INSERT 를 넣지 말 것
-- (FlywayBaselineV1GuardTest 가 막는다).
"""


def main(path: str) -> None:
    raw = open(path, encoding="utf-8").read()
    for pattern, label in FORBIDDEN:
        if pattern.search(raw):
            sys.exit(f"중단: 덤프에 {label} 가 있다. 베이스라인에 넣을 내용이 아니니 확인할 것.")

    # 주석 제거: /*...*/ (세션 설정 포함), -- 줄 주석
    text = re.sub(r"/\*.*?\*/", "", raw, flags=re.S)
    text = "\n".join(l for l in text.splitlines() if not l.lstrip().startswith("--"))

    tables = {}  # name -> statement (dump order 유지)
    for m in re.finditer(r"CREATE\s+TABLE\s+`([^`]+)`\s*\(.*?\)\s*ENGINE=[^;]*;", text, flags=re.S | re.I):
        name = m.group(1)
        if name in SKIP_TABLES:
            continue
        stmt = m.group(0)
        stmt = re.sub(r"\s+AUTO_INCREMENT=\d+", "", stmt)
        stmt = re.sub(r"\s+COLLATE=\w+", "", stmt)
        stmt = re.sub(r"\s+DEFAULT\s+COLLATE=\w+", "", stmt)
        tables[name] = stmt
    if not tables:
        sys.exit("중단: CREATE TABLE 을 하나도 찾지 못했다.")

    # CREATE TABLE 바깥에 남은 SQL 문이 있으면(DROP/LOCK/ALTER 등) 알 수 없는 내용이므로 확인
    leftover = text
    for m in re.finditer(r"CREATE\s+TABLE\s+`[^`]+`\s*\(.*?\)\s*ENGINE=[^;]*;", text, flags=re.S | re.I):
        leftover = leftover.replace(m.group(0), "")
    leftover = re.sub(r"DROP\s+TABLE\s+IF\s+EXISTS\s+`[^`]+`\s*;", "", leftover, flags=re.I)
    leftover = re.sub(r"^\s*;\s*$", "", leftover, flags=re.M).strip()  # 주석을 지우고 남은 ; 만 제거
    if leftover:
        sys.exit("중단: 예상 밖의 SQL 이 남아 있다:\n" + leftover[:500])

    # FK 부모 우선 위상 정렬 (덤프 순서를 최대한 유지)
    deps = {
        n: {r for r in re.findall(r"REFERENCES\s+`([^`]+)`", s) if r != n} for n, s in tables.items()
    }
    for n, d in deps.items():
        missing = d - tables.keys()
        if missing:
            sys.exit(f"중단: {n} 이 덤프에 없는 테이블을 참조한다: {sorted(missing)}")
    ordered, done = [], set()
    while len(ordered) < len(tables):
        progressed = False
        for n in tables:
            if n not in done and deps[n] <= done:
                ordered.append(n)
                done.add(n)
                progressed = True
        if not progressed:
            sys.exit("중단: FK 순환 참조가 있다: " + ", ".join(sorted(set(tables) - done)))

    out = [HEADER]
    for n in ordered:
        out.append(f"-- {n}\n{tables[n]}\n")
    sys.stdout.write("\n".join(out))


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
