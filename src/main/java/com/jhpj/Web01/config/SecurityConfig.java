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
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.authentication.DisabledException;

import javax.sql.DataSource;

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
    private final DataSource dataSource;

    /**
     * 자동 로그인(Remember Me) 토큰을 DB에 영속 저장하는 리포지토리 빈
     * - PERSISTENT_LOGINS 테이블 사용 (V12 마이그레이션으로 생성)
     * - 토큰 탈취 감지: 동일 series에 다른 토큰 감지 시 전체 세션 무효화
     */
    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        return repo;
    }

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
                .rememberMe(rememberMe -> rememberMe
                        // DB 기반 영속 토큰 방식 (쿠키 탈취 감지 가능)
                        .tokenRepository(persistentTokenRepository())
                        // 30일간 자동 로그인 유지
                        .tokenValiditySeconds(30 * 24 * 60 * 60)
                        .userDetailsService(userDetailsService)
                        // 로그인 폼의 체크박스 name 속성과 일치해야 함
                        .rememberMeParameter("remember-me")
                        .rememberMeCookieName("remember-me")
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/home")          // ← 로그아웃 후 홈으로
                        .invalidateHttpSession(true)
                        // 자동 로그인 쿠키도 함께 삭제
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )
                .userDetailsService(userDetailsService);

        return http.build();
    }
}