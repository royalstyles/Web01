package com.jhpj.Web01.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 게시판 카테고리 엔티티 — CATEGORIES 테이블 매핑
 * 관리자 페이지에서 추가/수정/삭제 가능
 * 카테고리 삭제 시 해당 게시글의 category_id 는 NULL 로 유지 (FK ON DELETE SET NULL)
 */
@Entity
@Table(name = "CATEGORIES")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Category {

    /** PK — Oracle CATEGORIES_SEQ 시퀀스로 자동 생성 */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "categories_seq")
    @SequenceGenerator(name = "categories_seq", sequenceName = "CATEGORIES_SEQ", allocationSize = 1)
    private Long id;

    /** 카테고리 이름 — 중복 불가, 최대 50자 */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** 목록 표시 순서 — 숫자가 낮을수록 앞에 표시 (기본값: 0) */
    @Column(nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}