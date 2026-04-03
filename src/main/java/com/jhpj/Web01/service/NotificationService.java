package com.jhpj.Web01.service;

import com.jhpj.Web01.entity.Notification;
import com.jhpj.Web01.entity.Post;
import com.jhpj.Web01.entity.User;
import com.jhpj.Web01.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 알림 서비스
 * 좋아요/댓글 발생 시 게시글 작성자에게 알림을 생성하고,
 * 알림 목록 조회 및 읽음 처리를 담당
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // 알림 메시지에서 게시글 제목 최대 길이
    private static final int TITLE_MAX_LEN = 25;

    // ── 알림 생성 ────────────────────────────────────────────

    /**
     * 좋아요 알림 생성
     * - 자신의 게시글에 자신이 누른 경우 알림 생성 안 함
     */
    @Transactional
    public void createLikeNotification(Post post, User actor) {
        // 본인 글에 본인 좋아요: 알림 제외
        if (post.getAuthor().getId().equals(actor.getId())) return;

        notificationRepository.save(Notification.builder()
                .recipient(post.getAuthor())
                .actor(actor)
                .post(post)
                .type(Notification.NotifType.LIKE)
                .message(actor.getUsername() + "님이 '"
                        + truncate(post.getTitle()) + "' 글에 좋아요를 눌렀습니다.")
                .build());
    }

    /**
     * 댓글 알림 생성
     * - 자신의 게시글에 자신이 댓글 단 경우 알림 생성 안 함
     */
    @Transactional
    public void createCommentNotification(Post post, User actor) {
        // 본인 글에 본인 댓글: 알림 제외
        if (post.getAuthor().getId().equals(actor.getId())) return;

        notificationRepository.save(Notification.builder()
                .recipient(post.getAuthor())
                .actor(actor)
                .post(post)
                .type(Notification.NotifType.COMMENT)
                .message(actor.getUsername() + "님이 '"
                        + truncate(post.getTitle()) + "' 글에 댓글을 달았습니다.")
                .build());
    }

    // ── 조회 ─────────────────────────────────────────────────

    /** 최근 30개 알림 조회 */
    @Transactional(readOnly = true)
    public List<Notification> getNotifications(String username) {
        return notificationRepository.findTop30ByRecipient(username);
    }

    /** 읽지 않은 알림 수 */
    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        return notificationRepository.countByRecipientUsernameAndReadFalse(username);
    }

    // ── 읽음 처리 ────────────────────────────────────────────

    /** 단건 읽음 처리 — 본인 알림만 처리 가능 */
    @Transactional
    public void markAsRead(Long id, String username) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getRecipient().getUsername().equals(username)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    /** 전체 읽음 처리 */
    @Transactional
    public void markAllAsRead(String username) {
        notificationRepository.markAllAsReadByUsername(username);
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────

    /** 게시글 제목을 최대 길이로 자르고 "..." 추가 */
    private String truncate(String title) {
        if (title == null) return "";
        return title.length() > TITLE_MAX_LEN
                ? title.substring(0, TITLE_MAX_LEN) + "..."
                : title;
    }
}
