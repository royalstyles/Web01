package com.jhpj.Web01.config;

import com.jhpj.Web01.service.CustomUserDetailsService;
import com.jhpj.Web01.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.DisabledException;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final LoginAttemptService loginAttemptService; // ✅ 추가

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/css/**", "/js/**").permitAll()  // 로그인·회원가입 허용
                        .requestMatchers("/admin/**").hasRole("ADMIN")                 // 관리자 전용
                        .anyRequest().authenticated()                                  // 나머지 로그인 필요
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
                            } else if (exception.getMessage() != null && exception.getMessage().contains("잠겼습니다")) {
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
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .userDetailsService(userDetailsService);

        return http.build();
    }
}
