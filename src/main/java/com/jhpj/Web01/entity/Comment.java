package com.jhpj.Web01.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 댓글 엔티티 — COMMENTS 테이블 매핑
 * 게시글(Post) 에 속하며, 게시글 삭제 시 CASCADE 로 함께 삭제
 * 작성자 본인 또는 관리자만 수정/삭제 가능 (BoardService 에서 권한 검사)
 */
@Entity
@Table(name = "COMMENTS")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Comment {

    /** PK — Oracle COMMENTS_SEQ 시퀀스로 자동 생성 */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comments_seq")
    @SequenceGenerator(name = "comments_seq", sequenceName = "COMMENTS_SEQ", allocationSize = 1)
    private Long id;

    /** 댓글이 달린 게시글 — LAZY 로딩 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /** 댓글 작성자 — 목록 조회 시 FETCH JOIN 으로 N+1 방지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    /** 댓글 본문 (최대 1000자) */
    @Column(nullable = false, length = 1000)
    private String content;

    /** 최초 작성 일시 — 수정 불가 */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 마지막 수정 일시 — @PreUpdate 로 자동 갱신 */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** 댓글 수정 직전 updatedAt 자동 갱신 */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}