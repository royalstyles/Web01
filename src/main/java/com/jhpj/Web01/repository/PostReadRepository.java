package com.jhpj.Web01.repository;

import com.jhpj.Web01.entity.PostRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * 게시글 읽음 이력 데이터 접근 레이어
 */
public interface PostReadRepository extends JpaRepository<PostRead, Long> {

    /** 특정 사용자가 특정 게시글을 읽었는지 확인 */
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    /**
     * 현재 목록 페이지의 post ID 목록 중 사용자가 읽은 ID 만 반환
     * IN 절로 현재 페이지(최대 10개)만 조회해 오버헤드 최소화
     */
    @Query("SELECT pr.post.id FROM PostRead pr WHERE pr.user.id = :userId AND pr.post.id IN :postIds")
    Set<Long> findReadPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
