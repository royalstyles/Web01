package com.jhpj.Web01.service;

import com.jhpj.Web01.entity.User;
import com.jhpj.Web01.repository.UserRepository;
import com.jhpj.Web01.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService; // ✅ 추가

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // ✅ 계정 잠금 확인
        if (loginAttemptService.isBlocked(username)) {
            long remaining = loginAttemptService.getRemainingLockSeconds(username);
            throw new LockedException("계정이 잠겼습니다. " + remaining + "초 후 다시 시도해주세요.");
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }

    /** 회원가입 — 비밀번호 정책 검증 포함 */
    public User register(String username, String password, String confirmPassword, String email) {
        // ✅ 비밀번호 정책 검증
        String validationError = PasswordValidator.validate(password, confirmPassword);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        return userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .role(User.Role.ROLE_USER)
                .build());
    }
}
