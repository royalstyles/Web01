package com.jhpj.Web01.repository;

import com.jhpj.Web01.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 게시판 카테고리 데이터 접근 레이어 — Spring Data JPA 자동 구현
 * 관리자 페이지 카테고리 관리 및 게시글 목록 필터에 사용
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** 정렬 순서(sortOrder) 오름차순으로 전체 카테고리 조회 — 헤더/필터 바에서 사용 */
    List<Category> findAllByOrderBySortOrderAsc();

    /** 카테고리 이름 중복 검사 — 추가/수정 시 동명 카테고리 방지 */
    boolean existsByName(String name);

    /** 카테고리 이름으로 조회 — 수정 시 자기 자신 제외 중복 검사에 사용 */
    Optional<Category> findByName(String name);
}