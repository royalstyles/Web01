package com.jhpj.Web01.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림 엔티티 — NOTIFICATIONS 테이블 매핑
 * 글 작성자에게 좋아요(LIKE) 또는 댓글(COMMENT) 발생 시 생성
 * - actor(발생자) 탈퇴 시 DB ON DELETE SET NULL 로 actor_id = null 처리
 * - post 삭제 시 DB ON DELETE CASCADE 로 알림도 함께 삭제
 */
@Entity
@Table(name = "NOTIFICATIONS")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifications_seq")
    @SequenceGenerator(name = "notifications_seq", sequenceName = "NOTIFICATIONS_SEQ", allocationSize = 1)
    private Long id;

    /** 알림을 받는 사람 — 게시글 작성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    /** 알림을 발생시킨 사람 — null 허용 (탈퇴 시 ON DELETE SET NULL) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    /** 관련 게시글 — null 허용 (게시글 삭제 시 ON DELETE CASCADE 로 알림도 삭제됨) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    /** 알림 유형 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotifType type;

    /** 표시할 메시지 — 예) "홍길동님이 '제목...' 글에 좋아요를 눌렀습니다." */
    @Column(nullable = false, length = 500)
    private String message;

    /** 읽음 여부 — false(미읽음) / true(읽음) */
    @Column(name = "IS_READ", nullable = false)
    @Builder.Default
    private boolean read = false;

    /** 생성 일시 */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 알림 유형 정의 */
    public enum NotifType {
        LIKE,    // 좋아요
        COMMENT  // 댓글
    }
}
