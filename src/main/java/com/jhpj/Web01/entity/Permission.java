package com.jhpj.Web01.entity;

/**
 * 커스텀 역할에 부여 가능한 세부 기능 권한 열거형
 * 관리자가 커스텀 역할을 생성할 때 이 권한들을 선택적으로 부여할 수 있음
 * Spring Security에서는 "PERM_" 접두사를 붙여 GrantedAuthority로 변환됨
 * (예: POST_DELETE_OTHERS → PERM_POST_DELETE_OTHERS)
 */
public enum Permission {

    /** 타인이 작성한 게시글 삭제 가능 — BoardService.deletePost() 에서 검증 */
    POST_DELETE_OTHERS("타인 게시글 삭제"),

    /** 타인이 작성한 댓글 삭제 가능 — BoardService.deleteComment() 에서 검증 */
    COMMENT_DELETE_OTHERS("타인 댓글 삭제"),

    /** 공지 작성 및 수정 가능 — /admin/notices/add, /admin/notices/{id}/update 접근 허용 */
    NOTICE_WRITE("공지 작성/수정"),

    /** 공지 삭제 가능 — /admin/notices/{id}/delete 접근 허용 */
    NOTICE_DELETE("공지 삭제"),

    /** 게시판 카테고리 추가/수정/삭제 가능 — /admin/categories/** 접근 허용 */
    CATEGORY_MANAGE("카테고리 관리");

    /** 관리자 화면에 표시될 한국어 이름 */
    private final String displayName;

    Permission(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
