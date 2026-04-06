package com.jhpj.Web01.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 커스텀 역할 엔티티 — CUSTOM_ROLES 테이블 매핑
 * 관리자가 생성/삭제 가능한 사용자 정의 역할
 * 각 역할에 Permission 목록을 부여하여 게시글 삭제, 공지 관리 등의 세부 기능 권한 제어
 *
 * 사용 흐름:
 * 1. 관리자가 커스텀 역할 생성 (예: "모더레이터")
 * 2. 권한 부여 (예: POST_DELETE_OTHERS, COMMENT_DELETE_OTHERS)
 * 3. 특정 회원에게 해당 역할 할당
 * 4. 해당 회원은 로그인 시 "PERM_POST_DELETE_OTHERS" 권한을 자동으로 보유
 */
@Entity
@Table(name = "CUSTOM_ROLES")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CustomRole {

    /** PK — CUSTOM_ROLES_SEQ 시퀀스로 자동 생성 */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "custom_roles_seq")
    @SequenceGenerator(name = "custom_roles_seq", sequenceName = "CUSTOM_ROLES_SEQ", allocationSize = 1)
    private Long id;

    /** 역할명 — 고유, 최대 50자 (예: 모더레이터, 서브관리자) */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** 역할 설명 — 선택 입력 */
    @Column(length = 200)
    private String description;

    /**
     * 이 역할에 부여된 세부 기능 권한 목록
     * CUSTOM_ROLE_PERMISSIONS 조인 테이블에 저장 (ROLE_ID, PERMISSION 컬럼)
     * EAGER 로드 필수 — User.getAuthorities() 가 트랜잭션 밖에서도 호출되기 때문
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "CUSTOM_ROLE_PERMISSIONS",
            joinColumns = @JoinColumn(name = "ROLE_ID")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "PERMISSION", length = 50)
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    /** 역할 생성 일시 */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
