package com.jhpj.Web01.repository;

import com.jhpj.Web01.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 게시글의 댓글 목록 (작성일 오름차순) — author fetch join으로 N+1 방지
    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.author " +
            "WHERE c.post.id = :postId " +
            "ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdWithAuthor(@Param("postId") Long postId);

    // 게시글 댓글 수
    long countByPostId(Long postId);
}