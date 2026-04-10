package com.jhpj.Web01.util;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;

/**
 * Spring Security UserDetails 기반 권한 체크 유틸리티
 * 컨트롤러 / ControllerAdvice에서 반복되는 stream 보일러플레이트를 대체
 *
 * 사용 예:
 *   boolean isAdmin        = AuthorityUtils.isAdmin(userDetails);
 *   boolean canEdit        = AuthorityUtils.hasAuthority(userDetails, "PERM_POST_EDIT_OTHERS");
 *   boolean hasAnyManage   = AuthorityUtils.hasAnyAuthority(userDetails, "PERM_NOTICE_WRITE", "PERM_CATEGORY_MANAGE");
 */
public final class AuthorityUtils {

    /** 인스턴스화 방지 */
    private AuthorityUtils() {}

    /**
     * ROLE_ADMIN 여부 확인
     */
    public static boolean isAdmin(UserDetails user) {
        return hasAuthority(user, "ROLE_ADMIN");
    }

    /**
     * 특정 권한 보유 여부 확인
     * @param authority 확인할 권한 문자열 (예: "PERM_NOTICE_WRITE", "ROLE_ADMIN")
     */
    public static boolean hasAuthority(UserDetails user, String authority) {
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }

    /**
     * 전달된 권한 중 하나라도 보유하면 true
     * @param authorities 확인할 권한 목록 (varargs)
     */
    public static boolean hasAnyAuthority(UserDetails user, String... authorities) {
        return Arrays.stream(authorities)
                .anyMatch(auth -> hasAuthority(user, auth));
    }
}
