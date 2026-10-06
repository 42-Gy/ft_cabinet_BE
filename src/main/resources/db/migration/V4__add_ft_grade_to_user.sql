-- 42 API 본과정(cursus 21)의 grade 원문. 트센 판정은 애플리케이션에서 이 값으로 한다.
-- 원문을 저장해 두면 등급 문자열 해석이 바뀌어도 모든 사용자가 다시 로그인하지 않고 코드만 고치면 된다.
-- NULL 은 "아직 모름/본과정 항목 없음" 이고, 일반 사용자로 취급된다.
ALTER TABLE `user`
    ADD COLUMN ft_grade VARCHAR(32) NULL;
