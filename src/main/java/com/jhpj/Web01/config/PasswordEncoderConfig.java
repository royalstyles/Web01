package com.jhpj.Web01.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 인코더 설정 클래스
 * SecurityConfig 와 순환 의존성을 피하기 위해 별도 @Configuration 으로 분리
 * BCryptPasswordEncoder 는 솔트를 포함한 단방향 해시를 사용해 안전하게 비밀번호를 저장
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCrypt 기반 PasswordEncoder 빈 등록
     * 회원가입/비밀번호 변경 시 encode(), 로그인 시 matches() 로 검증
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
