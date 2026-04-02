package com.jhpj.Web01.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 웹 요청 관련 공통 유틸리티
 */
public final class WebUtils {

    private WebUtils() {}

    /**
     * 요청 객체에서 스킴 + 호스트 + 포트를 조합해 baseUrl 을 추출
     * 예) https://example.com  /  http://localhost:8080
     * AuthController(회원가입), ProfileController(이메일 변경) 에서 인증 링크 생성 시 사용
     */
    public static String extractBaseUrl(HttpServletRequest request) {
        int port = request.getServerPort();
        boolean isDefaultPort = (port == 80 || port == 443);
        return request.getScheme() + "://" + request.getServerName()
                + (isDefaultPort ? "" : ":" + port);
    }
}
