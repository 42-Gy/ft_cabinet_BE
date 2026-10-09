# 42 API cursus_users 픽스처

실제 42 API 응답(2026-10-06 테스트 환경에서 직접 확인)에서 **판정에 쓰는 필드만** 옮기고, 로그인명·ID 등 개인 식별 정보는 넣지 않았다.

- `cursus-users-transcender.json`: 트센 계정. cursus 9 `Pisciner`, 21 `Transcender`(end_at/blackholed_at 모두 null), 66 grade 없음.
- `cursus-users-cadet.json`: 본과정 재학 계정. cursus 9 `Pisciner`, 21 `Cadet`, 66 grade 없음.

확인하지 못한 필드(예: cadet 의 `end_at`, `blackholed_at`)는 지어내지 않고 생략했다. 이 외의 값(미확인 grade, 항목 중복 등)은 테스트 코드 안에 **합성 데이터**로 따로 만든다.
