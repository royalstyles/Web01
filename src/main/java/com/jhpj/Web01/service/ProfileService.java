package com.jhpj.Web01.service;

import com.jhpj.Web01.entity.EmailChangeToken;
import com.jhpj.Web01.entity.PostFile;
import com.jhpj.Web01.entity.User;
import com.jhpj.Web01.repository.EmailChangeTokenRepository;
import com.jhpj.Web01.repository.UserRepository;
import com.jhpj.Web01.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 프로필 관리 서비스
 * 로그인한 사용자 본인의 아이디/비밀번호/이메일/프로필 이미지 변경을 처리
 * 이메일 변경은 새 이메일로 인증 링크를 발송하고 클릭 후 완료되는 2단계 방식
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final EmailChangeTokenRepository emailChangeTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final FileService fileService;

    /** username 으로 사용자 조회 — 없으면 IllegalArgumentException */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    /** 프로필 이미지 변경 */
    @Transactional
    public void updateProfileImage(String username, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일을 선택해주세요.");
        }

        User user = findByUsername(username);

        // 기존 이미지 삭제
        if (user.getProfileImage() != null) {
            fileService.deleteProfileImage(extractStoredName(user.getProfileImage()));
        }

        PostFile saved = fileService.uploadProfileImage(file, user);
        user.setProfileImage(saved.getFileUrl());
        userRepository.save(user);
    }

    /** 프로필 이미지 삭제 */
    @Transactional
    public void deleteProfileImage(String username) {
        User user = findByUsername(username);
        if (user.getProfileImage() == null) {
            throw new IllegalArgumentException("등록된 프로필 이미지가 없습니다.");
        }
        fileService.deleteProfileImage(extractStoredName(user.getProfileImage()));
        user.setProfileImage(null);
        userRepository.save(user);
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

    /**
     * 현재 비밀번호 일치 여부 확인 — 프로필 접근 전 본인 인증에 사용
     * @return true: 일치, false: 불일치
     */
    public boolean verifyPassword(String username, String rawPassword) {
        User user = findByUsername(username);
        return passwordEncoder.matches(rawPassword, user.getPassword());
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

    /** 프로필 이미지 URL 에서 저장 파일명만 추출 (예: /uploads/profiles/abc.jpg → abc.jpg) */
    private String extractStoredName(String imageUrl) {
        return imageUrl.replaceAll(".*/profiles/", "");
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