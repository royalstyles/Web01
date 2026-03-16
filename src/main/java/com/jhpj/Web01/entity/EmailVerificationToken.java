package com.jhpj.Web01.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "EMAIL_VERIFICATION_TOKENS")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "evt_seq")
    @SequenceGenerator(name = "evt_seq", sequenceName = "EVT_SEQ", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;  // 24시간 후 만료

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
