package com.jhpj.Web01.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 회원가입 이메일 인증 토큰 엔티티 — EMAIL_VERIFICATION_TOKENS 테이블 매핑
 * 회원가입 시 UUID 토큰을 생성해 인증 메일로 발송
 * 사용자가 링크를 클릭하면 AuthController.verifyEmail() 에서 인증 완료 처리 후 이 레코드 삭제
 */
@Entity
@Table(name = "EMAIL_VERIFICATION_TOKENS")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailVerificationToken {

    /** PK — Oracle EVT_SEQ 시퀀스로 자동 생성 */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "evt_seq")
    @SequenceGenerator(name = "evt_seq", sequenceName = "EVT_SEQ", allocationSize = 1)
    private Long id;

    /** 인증 URL 에 포함되는 UUID 토큰 — 중복 불가 */
    @Column(nullable = false, unique = true)
    private String token;

    /** 인증 대상 사용자 (1:1 관계) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 토큰 만료 시각 — 발급 후 24시간 유효 */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** 현재 시각이 만료 시각을 지났으면 true 반환 */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
