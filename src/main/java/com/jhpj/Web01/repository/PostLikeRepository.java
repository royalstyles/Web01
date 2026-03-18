package com.jhpj.Web01.repository;

import com.jhpj.Web01.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    // 해당 사용자가 이 게시글에 좋아요 눌렀는지 확인
    boolean existsByPostIdAndUserUsername(Long postId, String username);

    // 좋아요 취소 시 삭제용
    Optional<PostLike> findByPostIdAndUserUsername(Long postId, String username);

    // 게시글 좋아요 수
    long countByPostId(Long postId);
}