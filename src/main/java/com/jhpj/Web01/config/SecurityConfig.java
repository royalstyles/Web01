package com.jhpj.Web01.config;

import com.jhpj.Web01.service.CustomUserDetailsService;
import com.jhpj.Web01.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.DisabledException;

/**
 * Spring Security 보안 설정 클래스
 * - 경로별 접근 권한 정의 (공개/인증 필요/관리자 전용)
 * - 폼 로그인 / 로그아웃 동작 설정
 * - 로그인 실패 횟수 추적 및 계정 잠금 처리 (LoginAttemptService 연동)
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final LoginAttemptService loginAttemptService;

    /**
     * AuthenticationManager 빈 직접 노출 — 컨트롤러/서비스에서 직접 인증이 필요할 때 주입
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * HTTP 보안 규칙 정의 — 경로별 접근 권한, 로그인/로그아웃 설정
     * 구체적인 경로 패턴을 먼저 선언하고 와일드카드 패턴을 나중에 선언해야 의도대로 동작
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 1. 인증 필요 경로 먼저 (구체적인 것 우선)
                        // 관리자 전용
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 게시글 작성/수정/삭제/댓글/좋아요/파일업로드 — 로그인 필수
                        .requestMatchers(
                                "/board/write", "/board/*/edit", "/board/*/delete",
                                "/board/*/comments", "/board/comments/**",
                                "/board/*/like", "/api/upload/**",
                                "/profile/**", "/my-posts/**"
                        ).authenticated()

                        // 2. 공개 허용
                        // 비인증 허용 (홈, 게시글 목록/상세 공개)
                        .requestMatchers(
                                "/auth/**", "/css/**", "/js/**", "/uploads/**",
                                "/", "/home", "/board/**"
                        ).permitAll()

                        // 나머지 전부 로그인 필요
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")           // 커스텀 로그인 페이지
                        .loginProcessingUrl("/auth/login")  // POST 처리 URL
                        .defaultSuccessUrl("/home", true)
                        // ✅ 성공 핸들러 — 실패 기록 초기화
                        .successHandler((request, response, authentication) -> {
                            loginAttemptService.loginSucceeded(authentication.getName());
                            response.sendRedirect("/home");
                        })
                        // ✅ 실패 핸들러 — 실패 횟수 기록
                        .failureHandler((request, response, exception) -> {
                            String username = request.getParameter("username");
                            loginAttemptService.loginFailed(username);

                            String redirect;
                            if (exception instanceof DisabledException) {
                                redirect = "/auth/login?unverified=true";
                            } else if (exception.getMessage() != null
                                    && exception.getMessage().contains("잠겼습니다")) {
                                redirect = "/auth/login?locked=true";
                            } else {
                                redirect = "/auth/login?error=true";
                            }
                            response.sendRedirect(redirect);
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/home")          // ← 로그아웃 후 홈으로
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .userDetailsService(userDetailsService);

        return http.build();
    }
}