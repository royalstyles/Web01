package com.jhpj.Web01.repository;

import com.jhpj.Web01.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 알림 리포지토리
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 수신자 기준 최근 30개 알림 조회 (actor, post FETCH JOIN 으로 N+1 방지)
     */
    @Query("""
            SELECT n FROM Notification n
            LEFT JOIN FETCH n.actor
            LEFT JOIN FETCH n.post
            WHERE n.recipient.username = :username
            ORDER BY n.createdAt DESC
            FETCH FIRST 30 ROWS ONLY
            """)
    List<Notification> findTop30ByRecipient(@Param("username") String username);

    /** 읽지 않은 알림 수 */
    long countByRecipientUsernameAndReadFalse(String username);

    /** 전체 읽음 처리 */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipient.username = :username AND n.read = false")
    void markAllAsReadByUsername(@Param("username") String username);
}
