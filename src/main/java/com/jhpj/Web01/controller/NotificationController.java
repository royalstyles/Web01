package com.jhpj.Web01.controller;

import com.jhpj.Web01.entity.Notification;
import com.jhpj.Web01.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 알림 REST API 컨트롤러 — /api/notifications/**
 * 프론트엔드 유저 패널에서 AJAX 로 호출
 * - GET  /api/notifications         : 최근 30개 알림 목록
 * - GET  /api/notifications/count   : 읽지 않은 알림 수
 * - POST /api/notifications/{id}/read : 단건 읽음 처리
 * - POST /api/notifications/read-all  : 전체 읽음 처리
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd HH:mm");

    /** 알림 목록 조회 */
    @GetMapping
    public List<Map<String, Object>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {

        return notificationService.getNotifications(userDetails.getUsername())
                .stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    /** 읽지 않은 알림 수 */
    @GetMapping("/count")
    public Map<String, Long> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        return Map.of("count",
                notificationService.getUnreadCount(userDetails.getUsername()));
    }

    /** 단건 읽음 처리 */
    @PostMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAsRead(id, userDetails.getUsername());
    }

    /** 전체 읽음 처리 */
    @PostMapping("/read-all")
    public void markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getUsername());
    }

    /** Notification 엔티티를 JSON 응답용 Map 으로 변환 */
    private Map<String, Object> toMap(Notification n) {
        return Map.of(
                "id",        n.getId(),
                "type",      n.getType().name(),
                "message",   n.getMessage(),
                "read",      n.isRead(),
                "createdAt", n.getCreatedAt().format(FORMATTER),
                "postId",    n.getPost() != null ? n.getPost().getId() : ""
        );
    }
}
