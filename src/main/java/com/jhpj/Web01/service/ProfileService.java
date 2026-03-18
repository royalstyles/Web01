package com.jhpj.Web01.service;

import com.jhpj.Web01.entity.EmailChangeToken;
import com.jhpj.Web01.entity.User;
import com.jhpj.Web01.repository.EmailChangeTokenRepository;
import com.jhpj.Web01.repository.UserRepository;
import com.jhpj.Web01.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final EmailChangeTokenRepository emailChangeTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    /** 아이디(username) 변경 */
    @Transactional
    public void changeUsername(String currentUsername, String newUsername) {
        if (newUsername == null || newUsername.trim().length() < 4 || newUsername.trim().length() > 20) {
            throw new IllegalArgumentException("아이디는 4~20자여야 합니다.");
        }
        if (userRepository.existsByUsername(newUsername)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        User user = findByUsername(currentUsername);
        user.setUsername(newUsername.trim());
        userRepository.save(user);
    }

    /** 비밀번호 변경 */
    @Transactional
    public void changePassword(String username,
                               String currentPassword,
                               String newPassword,
                               String confirmNewPassword) {
        User user = findByUsername(username);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        String validationError = PasswordValidator.validate(newPassword, confirmNewPassword);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /** 이메일 변경 요청 — 새 이메일로 인증 링크 발송 */
    @Transactional
    public void requestEmailChange(String username, String newEmail, String baseUrl) {
        if (newEmail == null || newEmail.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }

        User user = findByUsername(username);

        if (user.getEmail().equalsIgnoreCase(newEmail)) {
            throw new IllegalArgumentException("현재 이메일과 동일합니다.");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 기존 변경 요청 토큰 삭제 (중복 요청 방지)
        emailChangeTokenRepository.deleteByUser_Id(user.getId());

        String token = UUID.randomUUID().toString();
        emailChangeTokenRepository.save(EmailChangeToken.builder()
                .token(token)
                .user(user)
                .newEmail(newEmail)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build());

        String verifyUrl = baseUrl + "/profile/email/verify?token=" + token;
        emailService.sendEmailChangeVerification(newEmail, user.getUsername(), verifyUrl);
    }

    /** 이메일 변경 인증 완료 처리 */
    @Transactional
    public String confirmEmailChange(String token) {
        EmailChangeToken ect = emailChangeTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("유효하지 않은 인증 링크입니다."));

        if (ect.isExpired()) {
            emailChangeTokenRepository.delete(ect);
            throw new IllegalStateException("인증 링크가 만료되었습니다. 다시 요청해주세요.");
        }

        // 최종 중복 체크 (요청 ~ 인증 사이에 다른 사용자가 같은 이메일로 가입했을 수 있음)
        if (userRepository.existsByEmail(ect.getNewEmail())) {
            emailChangeTokenRepository.delete(ect);
            throw new IllegalStateException("해당 이메일은 이미 사용 중입니다. 다시 요청해주세요.");
        }

        User user = ect.getUser();
        user.setEmail(ect.getNewEmail());
        userRepository.save(user);
        emailChangeTokenRepository.delete(ect);

        return ect.getNewEmail();
    }
}