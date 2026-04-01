package com.jhpj.Web01.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 회원 정보 엔티티 — USERS 테이블 매핑
 * Spring Security 의 UserDetails 를 직접 구현해 인증/인가에 사용
 * 이메일 인증(emailVerified=true) 완료 후에만 로그인 가능 (isEnabled 참조)
 */
@Entity
@Table(name = "USERS")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User implements UserDetails {

    /** PK — Oracle USERS_SEQ 시퀀스로 자동 생성 */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    @SequenceGenerator(name = "users_seq", sequenceName = "USERS_SEQ", allocationSize = 1)
    private Long id;

    /** 로그인 ID (4~50자, 중복 불가) */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** BCrypt 해시된 비밀번호 — 평문 저장 금지 */
    @Column(nullable = false)
    private String password;

    /** 인증 이메일 수신 주소 (중복 불가) */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** 권한 (기본: ROLE_USER, 관리자: ROLE_ADMIN) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    /** 회원가입 일시 — 수정 불가 */
    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 이메일 인증 완료 여부 — false 이면 로그인 차단(isEnabled) */
    @Column(nullable = false, columnDefinition = "number(1,0) default 0 not null")
    private boolean emailVerified = false;

    /** 프로필 이미지 URL (/uploads/profiles/uuid.jpg) — null 이면 기본 아바타 표시 */
    @Column(length = 500)
    private String profileImage;

    // ── UserDetails 구현 — Spring Security 인증 체계와 연동 ──────────────────────────

    /** role 필드를 Spring Security GrantedAuthority 목록으로 변환 */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    /** 계정 만료 없음 */
    @Override public boolean isAccountNonExpired()    { return true; }
    /** 계정 잠금 없음 (LoginAttemptService 에서 별도 관리) */
    @Override public boolean isAccountNonLocked()     { return true; }
    /** 자격증명 만료 없음 */
    @Override public boolean isCredentialsNonExpired(){ return true; }
    /** 이메일 인증 완료 여부 = 로그인 허용 여부 */
    @Override public boolean isEnabled() { return emailVerified; }

    /** 사용자 권한 정의 */
    public enum Role {
        ROLE_USER,   // 일반 회원
        ROLE_ADMIN   // 관리자 (게시글/댓글/회원 관리 가능)
    }
}