package com.jhpj.Web01.repository;

import com.jhpj.Web01.entity.PostFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostFileRepository extends JpaRepository<PostFile, Long> {

    // 게시글에 연결된 파일 목록
    List<PostFile> findByPostId(Long postId);

    // 저장 파일명으로 조회 (중복 방지, 삭제 시 사용)
    Optional<PostFile> findByStoredName(String storedName);

    // 임시 파일 (post_id = null) 목록 — 고아 파일 정리 스케줄러용
    @Query("SELECT f FROM PostFile f WHERE f.post IS NULL AND f.createdAt < :before")
    List<PostFile> findOrphanFiles(@Param("before") LocalDateTime before);

    // 업로더 기준 임시 파일 조회 (게시글 작성 취소 시 정리)
    List<PostFile> findByPostIsNullAndUploaderUsername(String username);

    // 게시글의 파일 전체 삭제 (CASCADE로 처리되지만 직접 삭제 시 사용)
    @Modifying
    @Query("DELETE FROM PostFile f WHERE f.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}