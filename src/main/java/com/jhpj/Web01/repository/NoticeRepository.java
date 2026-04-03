package com.jhpj.Web01.repository;

import com.jhpj.Web01.entity.Notice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 공지 리포지토리
 */
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * 전체 공지 목록 — sortOrder 오름차순 (관리자 화면용, 숨김 포함)
     * @EntityGraph 로 categories IN-MEMORY 초기화 — SQL DISTINCT 없이 CLOB 충돌 회피
     */
    @EntityGraph(attributePaths = {"categories"})
    List<Notice> findAllByOrderBySortOrderAsc();

    /**
     * 전체 게시판 보기용 — categories 가 비어있는(전체 지정) 활성 공지만
     */
    @Query("SELECT n FROM Notice n WHERE n.active = true AND n.categories IS EMPTY ORDER BY n.sortOrder ASC")
    List<Notice> findByActiveTrueAndCategoriesEmpty();

    /**
     * 특정 카테고리 보기용 — categories 비어있는(전체) 공지 + 해당 카테고리 포함 공지
     * EXISTS 서브쿼리로 JOIN/DISTINCT 없이 처리 — Oracle CLOB+DISTINCT 충돌 회피
     */
    @Query("""
            SELECT n FROM Notice n
            WHERE n.active = true
              AND (n.categories IS EMPTY
                   OR EXISTS (SELECT c FROM n.categories c WHERE c.id = :categoryId))
            ORDER BY n.sortOrder ASC
            """)
    List<Notice> findActiveByCategoryId(@Param("categoryId") Long categoryId);

    /** 현재 가장 큰 sortOrder 값 */
    Optional<Notice> findTopByOrderBySortOrderDesc();

    /** 위로 이동 대상: 현재보다 sortOrder 가 낮은 것 중 가장 큰 것 */
    Optional<Notice> findTopBySortOrderLessThanOrderBySortOrderDesc(int sortOrder);

    /** 아래로 이동 대상: 현재보다 sortOrder 가 큰 것 중 가장 작은 것 */
    Optional<Notice> findTopBySortOrderGreaterThanOrderBySortOrderAsc(int sortOrder);
}
