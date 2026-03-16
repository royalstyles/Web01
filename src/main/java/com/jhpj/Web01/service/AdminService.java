package com.jhpj.Web01.service;

import com.jhpj.Web01.entity.User;
import com.jhpj.Web01.repository.EmailVerificationTokenRepository;
import com.jhpj.Web01.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final LoginAttemptService loginAttemptService;

    /** 권한 토글: ROLE_USER ↔ ROLE_ADMIN */
    @Transactional
    public void toggleRole(Long userId) {
        User user = findUser(userId);
        user.setRole(
                user.getRole() == User.Role.ROLE_ADMIN
                        ? User.Role.ROLE_USER
                        : User.Role.ROLE_ADMIN
        );
        userRepository.save(user);
    }

    /** 회원 삭제 (이메일 인증 토큰 먼저 삭제 → FK 제거) */
    @Transactional
    public void deleteUser(Long userId) {
        tokenRepository.deleteByUser_Id(userId);
        userRepository.deleteById(userId);
    }

    /** 로그인 잠금 해제 */
    @Transactional
    public void unlockUser(Long userId) {
        User user = findUser(userId);
        loginAttemptService.loginSucceeded(user.getUsername());
    }

    /** 이메일 인증 강제 완료 */
    @Transactional
    public void forceVerify(Long userId) {
        User user = findUser(userId);
        user.setEmailVerified(true);
        tokenRepository.deleteByUser_Id(userId); // 미사용 토큰 정리
        userRepository.save(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }
}