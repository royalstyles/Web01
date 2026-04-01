package com.jhpj.Web01.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 게시글 좋아요 엔티티 — POST_LIKES 테이블 매핑
 * (post_id, user_id) 복합 유니크 제약으로 동일 게시글에 중복 좋아요 방지
 * BoardService.toggleLike() 에서 존재 여부에 따라 추가/삭제로 토글 처리
 * Oracle 트리거가 이 테이블의 INSERT/DELETE 를 감지해 POSTS.like_count 를 자동 동기화
 */
@Entity
@Table(
        name = "POST_LIKES",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_LIKES",
                columnNames = {"post_id", "user_id"} // 동일 사용자의 중복 좋아요 방지
        )
)
@Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PostLike {

    /** PK — Oracle POST_LIKES_SEQ 시퀀스로 자동 생성 */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_likes_seq")
    @SequenceGenerator(name = "post_likes_seq", sequenceName = "POST_LIKES_SEQ", allocationSize = 1)
    private Long id;

    /** 좋아요가 달린 게시글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /** 좋아요를 누른 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 좋아요 누른 시각 — 수정 불가 */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}