package com.jhpj.Web01.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 공지 엔티티 — NOTICES 테이블 매핑
 * 관리자가 작성하며 게시판 목록 상단에 sortOrder 오름차순으로 고정 표시
 * active=false 로 설정하면 목록에 노출되지 않음 (삭제 없이 숨김 가능)
 * categories 가 비어있으면 전체 게시판에 표시, 값이 있으면 해당 카테고리에서만 표시
 */
@Entity
@Table(name = "NOTICES")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notices_seq")
    @SequenceGenerator(name = "notices_seq", sequenceName = "NOTICES_SEQ", allocationSize = 1)
    private Long id;

    /** 작성자 (관리자) — 탈퇴 시 ON DELETE CASCADE 로 공지도 삭제 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** 공지 제목 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 공지 본문 (Oracle CLOB) */
    @Lob
    @Column(columnDefinition = "CLOB")
    private String content;

    /**
     * 노출 게시판 목록 (다대다)
     * 비어있으면 전체 게시판에 표시
     * 값이 있으면 선택된 카테고리 게시판에서만 표시
     * 카테고리 삭제 시 ON DELETE CASCADE 로 조인 행 자동 삭제
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "NOTICE_CATEGORIES",
            joinColumns = @JoinColumn(name = "NOTICE_ID"),
            inverseJoinColumns = @JoinColumn(name = "CATEGORY_ID")
    )
    @Builder.Default
    private Set<Category> categories = new HashSet<>();

    /** 표시 순서 — 낮을수록 상단에 표시 */
    @Column(nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    /** 노출 여부 — false 이면 게시판 목록에 표시하지 않음 */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** 작성 일시 */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 수정 일시 */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
