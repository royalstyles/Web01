package com.jhpj.Web01.repository;

import com.jhpj.Web01.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 게시글 데이터 접근 레이어 — Spring Data JPA 자동 구현
 * 모든 목록 조회 쿼리에 LEFT JOIN FETCH p.author, p.category 를 포함해
 * 게시글 목록 렌더링 시 발생하는 N+1 쿼리 문제를 방지
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /** 전체 게시글 목록 — 최신순, 작성자/카테고리 FETCH JOIN */
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category ORDER BY p.createdAt DESC")
    Page<Post> findAllWithDetails(Pageable pageable);

    /** 특정 카테고리의 게시글 목록 — 최신순 */
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE p.category.id = :categoryId ORDER BY p.createdAt DESC")
    Page<Post> findByCategoryWithDetails(@Param("categoryId") Long categoryId, Pageable pageable);

    /** 제목 키워드 검색 (대소문자 무시) */
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE UPPER(p.title) LIKE UPPER(CONCAT('%', :keyword, '%')) ORDER BY p.createdAt DESC")
    Page<Post> findByKeywordWithDetails(@Param("keyword") String keyword, Pageable pageable);

    /** 카테고리 + 제목 키워드 복합 검색 */
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE p.category.id = :categoryId AND UPPER(p.title) LIKE UPPER(CONCAT('%', :keyword, '%')) ORDER BY p.createdAt DESC")
    Page<Post> findByCategoryAndKeywordWithDetails(@Param("categoryId") Long categoryId, @Param("keyword") String keyword, Pageable pageable);

    /** 작성자 이름 키워드 검색 (대소문자 무시) */
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE UPPER(p.author.username) LIKE UPPER(CONCAT('%', :keyword, '%')) ORDER BY p.createdAt DESC")
    Page<Post> findByAuthorKeywordWithDetails(@Param("keyword") String keyword, Pageable pageable);

    /** 카테고리 + 작성자 이름 복합 검색 */
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE p.category.id = :categoryId AND UPPER(p.author.username) LIKE UPPER(CONCAT('%', :keyword, '%')) ORDER BY p.createdAt DESC")
    Page<Post> findByCategoryAndAuthorKeywordWithDetails(@Param("categoryId") Long categoryId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 제목+본문 키워드 검색
     * - 제목: UPPER() 적용해 대소문자 무시
     * - 본문(CLOB): Hibernate 6에서 UPPER()를 CLOB에 적용 불가 → plain LIKE 사용
     *   (한국어 콘텐츠는 대소문자 구분 없으므로 실사용상 문제없음)
     */
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE UPPER(p.title) LIKE UPPER(CONCAT('%', :keyword, '%')) OR p.content LIKE CONCAT('%', :keyword, '%') ORDER BY p.createdAt DESC")
    Page<Post> findByTitleOrContentKeywordWithDetails(@Param("keyword") String keyword, Pageable pageable);

    /** 카테고리 + 제목+본문 복합 검색 */
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE p.category.id = :categoryId AND (UPPER(p.title) LIKE UPPER(CONCAT('%', :keyword, '%')) OR p.content LIKE CONCAT('%', :keyword, '%')) ORDER BY p.createdAt DESC")
    Page<Post> findByCategoryAndTitleOrContentKeywordWithDetails(@Param("categoryId") Long categoryId, @Param("keyword") String keyword, Pageable pageable);

    /** 작성자별 게시글 수 — 프로필/관리자 통계에 사용 */
    long countByAuthorUsername(String username);

    /** 내가 쓴 글 목록 조회 — 특정 작성자의 게시글만 최신순으로 반환 (MyPostsController 에서 사용) */
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE p.author.username = :username ORDER BY p.createdAt DESC")
    Page<Post> findByAuthorUsernameWithDetails(@Param("username") String username, Pageable pageable);

    /** 외부 수집 중복 방지 — source_url 이 이미 존재하면 true */
    boolean existsBySourceUrl(String sourceUrl);

    /** 게시글 상세 조회 — 작성자/카테고리 FETCH JOIN 으로 Lazy 로딩 없이 한 번에 조회 */
    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.category " +
            "WHERE p.id = :id")
    java.util.Optional<Post> findByIdWithDetails(@Param("id") Long id);
}
