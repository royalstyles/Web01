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

    /** 타인이 작성한 게시글 수정 가능 — BoardService.checkAuthor() 에서 검증 */
    POST_EDIT_OTHERS("타인 게시글 수정"),

    /** 타인이 작성한 댓글 삭제 가능 — BoardService.deleteComment() 에서 검증 */
    COMMENT_DELETE_OTHERS("타인 댓글 삭제"),

    /** 공지 작성 및 수정 가능 — /admin/notices/add, /admin/notices/{id}/update 접근 허용 */
    NOTICE_WRITE("공지 작성/수정"),

    /** 공지 삭제 가능 — /admin/notices/{id}/delete 접근 허용 */
    NOTICE_DELETE("공지 삭제"),

    /** 게시판 카테고리 추가/수정/삭제 가능 — /admin/categories/** 접근 허용 */
    CATEGORY_MANAGE("카테고리 관리"),

    /** 회원 계정 잠금 해제 가능 — /admin/users/{id}/unlock 접근 허용 */
    USER_LOCK_MANAGE("회원 잠금/해제"),

    /** 회원 이메일 인증 강제 처리 가능 — /admin/users/{id}/verify 접근 허용 */
    USER_VERIFY_MANAGE("이메일 인증 처리"),

    /** 커스텀 역할 목록 및 할당 회원 조회 가능 — /admin/roles/* users GET 접근 허용 */
    CUSTOM_ROLE_VIEW("커스텀 역할 목록 조회"),

    /** 유저 패널의 로또 조합기 버튼 표시 여부 — 이 권한이 없으면 버튼이 숨겨짐 */
    LOTTO_ACCESS("로또 생성기");

    /** 관리자 화면에 표시될 한국어 이름 */
    private final String displayName;

    Permission(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
