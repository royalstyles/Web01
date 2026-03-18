package com.jhpj.Web01.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "EMAIL_CHANGE_TOKENS")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailChangeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ect_seq")
    @SequenceGenerator(name = "ect_seq", sequenceName = "ECT_SEQ", allocationSize = 1)
    private Long id;

    /** 인증용 UUID 토큰 */
    @Column(nullable = false, unique = true)
    private String token;

    /** 변경을 요청한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 변경하려는 새 이메일 */
    @Column(nullable = false, length = 100)
    private String newEmail;

    /** 만료 시각 (요청 후 24시간) */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}