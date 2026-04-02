package com.jhpj.Web01.repository;

import com.jhpj.Web01.entity.PostRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * 게시글 읽음 이력 데이터 접근 레이어
 */
public interface PostReadRepository extends JpaRepository<PostRead, Long> {

    /**
     * 읽음 기록 UPSERT — Oracle MERGE INTO 사용
     * 이미 읽은 경우(UNIQUE 제약)에도 예외 없이 무시 처리
     * saveAndFlush() + try-catch 방식은 Hibernate Session이 rollback-only로 마킹되는 문제가 있어
     * 이 방식으로 대체
     */
    @Modifying(clearAutomatically = true)
    @Query(value =
            "MERGE INTO POST_READS pr " +
            "USING DUAL " +
            "ON (pr.POST_ID = :postId AND pr.USER_ID = :userId) " +
            "WHEN NOT MATCHED THEN " +
            "  INSERT (ID, POST_ID, USER_ID, READ_AT) " +
            "  VALUES (POST_READS_SEQ.NEXTVAL, :postId, :userId, SYSTIMESTAMP)",
            nativeQuery = true)
    void mergeRead(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * 현재 목록 페이지의 post ID 목록 중 사용자가 읽은 ID 만 반환
     * IN 절로 현재 페이지(최대 10개)만 조회해 오버헤드 최소화
     */
    @Query("SELECT pr.post.id FROM PostRead pr WHERE pr.user.id = :userId AND pr.post.id IN :postIds")
    Set<Long> findReadPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
