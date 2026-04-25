-- FULLTEXT 인덱스: 숙소 이름/단문설명/설명에 대한 시맨틱 검색 지원
-- ngram 파서: 한국어·일본어·중국어 등 띄어쓰기 없는 언어에서도 토큰 분리
--
-- 최초 1회 실행 (운영 DB 및 개발 DB 모두 적용 필요):
ALTER TABLE rooms
    ADD FULLTEXT INDEX ft_room_search (name, short_description, description)
    WITH PARSER ngram;

-- MySQL my.cnf / my.ini에서 ngram 토큰 크기 조정 (선택, 기본값 2):
-- [mysqld]
-- ngram_token_size = 2
