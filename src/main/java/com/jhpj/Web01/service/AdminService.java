package com.jhpj.Web01.service;

import com.jhpj.Web01.entity.Category;
import com.jhpj.Web01.entity.User;
import com.jhpj.Web01.repository.CategoryRepository;
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
    private final CategoryRepository categoryRepository;  // 추가

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

    // ── 카테고리 추가 ──────────────────────────────────────────
    @Transactional
    public void addCategory(String name, int sortOrder) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("카테고리 이름을 입력해주세요.");
        }
        if (categoryRepository.existsByName(name.trim())) {
            throw new IllegalArgumentException("이미 존재하는 카테고리입니다: " + name);
        }
        categoryRepository.save(Category.builder()
                .name(name.trim())
                .sortOrder(sortOrder)
                .build());
    }

    // ── 카테고리 이름/순서 수정 ────────────────────────────────
    @Transactional
    public void updateCategory(Long categoryId, String name, int sortOrder) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("카테고리 이름을 입력해주세요.");
        }
        // 이름 중복 체크 (자기 자신 제외)
        categoryRepository.findByName(name.trim())
                .filter(c -> !c.getId().equals(categoryId))
                .ifPresent(c -> { throw new IllegalArgumentException("이미 존재하는 카테고리입니다: " + name); });

        category.setName(name.trim());
        category.setSortOrder(sortOrder);
        categoryRepository.save(category);
    }

    // ── 카테고리 삭제 ──────────────────────────────────────────
    @Transactional
    public void deleteCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("카테고리를 찾을 수 없습니다.");
        }
        // FK_POSTS_CATEGORY ON DELETE SET NULL 이므로 게시글은 카테고리 없음으로 유지됨
        categoryRepository.deleteById(categoryId);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }
}