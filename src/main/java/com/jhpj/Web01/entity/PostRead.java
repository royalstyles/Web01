package com.jhpj.Web01.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 게시글 읽음 이력 엔티티 — POST_READS 테이블 매핑
 * (post_id, user_id) 복합 유니크 제약으로 동일 게시글 중복 기록 방지
 * BoardService.markAsRead() 에서 미읽음 상태일 때만 INSERT
 * 기기 간 읽음 동기화에 사용 (게시판 목록에서 읽음/안읽음 표시)
 */
@Entity
@Table(
        name = "POST_READS",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_READS",
                columnNames = {"post_id", "user_id"}
        )
)
@Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PostRead {

    /** PK — Oracle POST_READS_SEQ 시퀀스로 자동 생성 */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_reads_seq")
    @SequenceGenerator(name = "post_reads_seq", sequenceName = "POST_READS_SEQ", allocationSize = 1)
    private Long id;

    /** 읽은 게시글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /** 읽은 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 최초 읽은 시각 — 수정 불가 */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime readAt = LocalDateTime.now();
}
