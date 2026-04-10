package com.jhpj.Web01.config;

import com.jhpj.Web01.service.CustomUserDetailsService;
import com.jhpj.Web01.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
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
@EnableMethodSecurity  // @PreAuthorize / @PostAuthorize 사용 가능
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
                        // ── 1. 관리자 전용 경로 (구체적인 것 먼저) ─────────────────
                        // 회원 잠금 해제 — 관리자 또는 USER_LOCK_MANAGE 권한 보유자
                        .requestMatchers("/admin/users/*/unlock").access(
                                new WebExpressionAuthorizationManager(
                                        "hasRole('ADMIN') or hasAuthority('PERM_USER_LOCK_MANAGE')"
                                )
                        )
                        // 이메일 인증 강제 — 관리자 또는 USER_VERIFY_MANAGE 권한 보유자
                        .requestMatchers("/admin/users/*/verify").access(
                                new WebExpressionAuthorizationManager(
                                        "hasRole('ADMIN') or hasAuthority('PERM_USER_VERIFY_MANAGE')"
                                )
                        )
                        // 커스텀 역할 목록·할당 회원 조회 — 관리자 또는 CUSTOM_ROLE_VIEW 권한 보유자
                        .requestMatchers(HttpMethod.GET, "/admin/roles/*/users").access(
                                new WebExpressionAuthorizationManager(
                                        "hasRole('ADMIN') or hasAuthority('PERM_CUSTOM_ROLE_VIEW')"
                                )
                        )
                        // 나머지 회원 관리, 커스텀 역할 관리, 외부 수집은 관리자만
                        .requestMatchers("/admin/users/**").hasRole("ADMIN")
                        .requestMatchers("/admin/roles/**").hasRole("ADMIN")
                        .requestMatchers("/admin/import/**").hasRole("ADMIN")

                        // 공지 관리 — 관리자 또는 공지 작성/삭제 권한 보유자
                        .requestMatchers("/admin/notices/**").access(
                                new WebExpressionAuthorizationManager(
                                        "hasRole('ADMIN') or hasAuthority('PERM_NOTICE_WRITE') or hasAuthority('PERM_NOTICE_DELETE')"
                                )
                        )
                        // 카테고리 관리 — 관리자 또는 카테고리 관리 권한 보유자
                        .requestMatchers("/admin/categories/**").access(
                                new WebExpressionAuthorizationManager(
                                        "hasRole('ADMIN') or hasAuthority('PERM_CATEGORY_MANAGE')"
                                )
                        )
                        // 관리자 대시보드 — 관리자 또는 특정 관리 권한 보유자
                        .requestMatchers("/admin", "/admin/").access(
                                new WebExpressionAuthorizationManager(
                                        "hasRole('ADMIN') or hasAuthority('PERM_NOTICE_WRITE') or " +
                                        "hasAuthority('PERM_NOTICE_DELETE') or hasAuthority('PERM_CATEGORY_MANAGE') or " +
                                        "hasAuthority('PERM_USER_LOCK_MANAGE') or hasAuthority('PERM_USER_VERIFY_MANAGE') or " +
                                        "hasAuthority('PERM_CUSTOM_ROLE_VIEW')"
                                )
                        )
                        // 나머지 /admin/** 은 관리자만
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // ── 2. 로그인 필수 경로 ─────────────────────────────────────
                        .requestMatchers(
                                "/board/write", "/board/*/edit", "/board/*/delete",
                                "/board/*/comments", "/board/comments/**",
                                "/board/*/like", "/api/upload/**",
                                "/profile/**", "/my-posts/**"
                        ).authenticated()

                        // ── 3. 공개 허용 ─────────────────────────────────────────────
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