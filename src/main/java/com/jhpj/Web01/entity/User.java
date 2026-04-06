package com.jhpj.Web01.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * 커스텀 역할 목록 — 여러 역할을 동시에 보유 가능 (다대다)
     * USER_CUSTOM_ROLES 조인 테이블로 관리
     * EAGER 로드 필수 — getAuthorities() 가 트랜잭션 없는 컨텍스트에서도 호출되기 때문
     * ON DELETE CASCADE: 역할 삭제 시 조인 테이블의 연결 행 자동 제거
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "USER_CUSTOM_ROLES",
            joinColumns = @JoinColumn(name = "USER_ID"),
            inverseJoinColumns = @JoinColumn(name = "ROLE_ID")
    )
    @Builder.Default
    private Set<CustomRole> customRoles = new HashSet<>();

    // ── UserDetails 구현 — Spring Security 인증 체계와 연동 ──────────────────────────

    /**
     * role 필드와 보유한 모든 커스텀 역할의 권한을 Spring Security GrantedAuthority 목록으로 변환
     * - 기본 역할: ROLE_USER 또는 ROLE_ADMIN
     * - 커스텀 역할: ROLE_{역할명} (예: ROLE_모더레이터) — 여러 개 가능
     * - 세부 권한: PERM_{권한명} (예: PERM_POST_DELETE_OTHERS) — 모든 역할의 권한 합산
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // 기본 시스템 역할 추가
        authorities.add(new SimpleGrantedAuthority(role.name()));

        // 보유한 모든 커스텀 역할의 역할명과 세부 권한을 합산
        for (CustomRole customRole : customRoles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + customRole.getName()));
            customRole.getPermissions().forEach(p ->
                    authorities.add(new SimpleGrantedAuthority("PERM_" + p.name()))
            );
        }

        return authorities;
    }

    /**
     * 보유한 커스텀 역할 중 하나라도 특정 세부 기능 권한이 부여되어 있는지 확인
     * @param permission 확인할 권한
     * @return 하나 이상의 커스텀 역할에 해당 권한이 있으면 true
     */
    public boolean hasPermission(Permission permission) {
        return customRoles.stream()
                .anyMatch(r -> r.getPermissions().contains(permission));
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