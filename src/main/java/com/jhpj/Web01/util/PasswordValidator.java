package com.jhpj.Web01.util;

public class PasswordValidator {

    /**
     * 비밀번호 정책 검증
     * - 최소 8자 이상
     * - 영문 + 숫자 + 특수문자 조합
     */
    public static String validate(String password, String confirmPassword) {

        // 1. 비밀번호 확인 일치
        if (!password.equals(confirmPassword)) {
            return "비밀번호가 일치하지 않습니다.";
        }
        // 2. 최소 8자
        if (password.length() < 8) {
            return "비밀번호는 8자 이상이어야 합니다.";
        }
        // 3. 영문 포함
        if (!password.matches(".*[a-zA-Z].*")) {
            return "비밀번호에 영문자를 포함해야 합니다.";
        }
        // 4. 숫자 포함
        if (!password.matches(".*[0-9].*")) {
            return "비밀번호에 숫자를 포함해야 합니다.";
        }
        // 5. 특수문자 포함
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            return "비밀번호에 특수문자를 포함해야 합니다.";
        }

        return null; // 통과
    }
}