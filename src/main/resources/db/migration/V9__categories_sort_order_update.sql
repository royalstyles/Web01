-- 카테고리 이름 수정 허용을 위한 제약 없음 (기존 구조 유지)
-- sort_order 인덱스 추가 (정렬 성능)
CREATE INDEX IDX_CATEGORIES_SORT ON CATEGORIES(SORT_ORDER ASC);