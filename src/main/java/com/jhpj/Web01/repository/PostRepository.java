package com.jhpj.Web01.repository;

import com.jhpj.Web01.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 기존 메서드들을 아래처럼 교체
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category ORDER BY p.createdAt DESC")
    Page<Post> findAllWithDetails(Pageable pageable);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE p.category.id = :categoryId ORDER BY p.createdAt DESC")
    Page<Post> findByCategoryWithDetails(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE UPPER(p.title) LIKE UPPER(CONCAT('%', :keyword, '%')) ORDER BY p.createdAt DESC")
    Page<Post> findByKeywordWithDetails(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE p.category.id = :categoryId AND UPPER(p.title) LIKE UPPER(CONCAT('%', :keyword, '%')) ORDER BY p.createdAt DESC")
    Page<Post> findByCategoryAndKeywordWithDetails(@Param("categoryId") Long categoryId, @Param("keyword") String keyword, Pageable pageable);

    // 작성자별 게시글 수
    long countByAuthorUsername(String username);

    // fetch join — 목록 조회 시 author, category N+1 방지
    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.category " +
            "WHERE p.id = :id")
    java.util.Optional<Post> findByIdWithDetails(@Param("id") Long id);
}