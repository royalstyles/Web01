package com.jhpj.Web01.service;

import com.jhpj.Web01.entity.Category;
import com.jhpj.Web01.entity.CustomRole;
import com.jhpj.Web01.entity.Notice;
import com.jhpj.Web01.entity.Permission;
import com.jhpj.Web01.entity.User;
import com.jhpj.Web01.repository.CategoryRepository;
import com.jhpj.Web01.repository.CustomRoleRepository;
import com.jhpj.Web01.repository.EmailVerificationTokenRepository;
import com.jhpj.Web01.repository.NoticeRepository;
import com.jhpj.Web01.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 관리자 기능 서비스
 * 회원 권한 변경, 회원 삭제, 계정 잠금 해제, 이메일 인증 강제 완료를 처리
 * 게시판 카테고리 추가/수정/삭제도 담당 (AdminController 에서 호출)
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final LoginAttemptService loginAttemptService;
    private final CategoryRepository categoryRepository;
    private final NoticeRepository noticeRepository;
    private final CustomRoleRepository customRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /** 임시 비밀번호 생성에 사용할 문자 집합 — 혼동 가능한 문자(0/O, 1/l/I) 제외 */
    private static final String TEMP_PW_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int TEMP_PW_LENGTH   = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 관리자 대시보드 통계 집계 결과
     * @param totalUsers      전체 회원 수
     * @param adminCount      관리자 수
     * @param unverifiedCount 이메일 미인증 수
     * @param lockedCount     계정 잠금 수
     * @param lockedIds       잠금된 회원 ID Set (Thymeleaf 조건 렌더링용)
     */
    public record DashboardStats(
            long totalUsers,
            long adminCount,
            long unverifiedCount,
            long lockedCount,
            Set<Long> lockedIds
    ) {}

    /**
     * 관리자 대시보드 통계 집계
     * loginAttemptService 가 이미 주입되어 있으므로 서비스 레이어에서 처리
     */
    @Transactional(readOnly = true)
    public DashboardStats calcDashboardStats() {
        List<User> users = userRepository.findAll();
        long totalUsers      = users.size();
        long adminCount      = users.stream().filter(u -> u.getRole() == User.Role.ROLE_ADMIN).count();
        long unverifiedCount = users.stream().filter(u -> !u.isEmailVerified()).count();

        Set<Long> lockedIds = users.stream()
                .filter(u -> loginAttemptService.isBlocked(u.getUsername()))
                .map(User::getId)
                .collect(Collectors.toSet());

        return new DashboardStats(totalUsers, adminCount, unverifiedCount, lockedIds.size(), lockedIds);
    }

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

    /**
     * 회원 비밀번호 초기화
     * 무작위 임시 비밀번호를 생성해 DB에 저장하고 회원 이메일로 발송
     * @return 발송된 이메일 주소 (성공 메시지 표시용)
     */
    @Transactional
    public String resetPassword(Long userId) {
        User user = findUser(userId);

        // 임시 비밀번호 생성 (SecureRandom 기반, 혼동 문자 제외)
        String tempPassword = generateTempPassword();

        // BCrypt 해시 후 저장
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        // 회원 이메일로 임시 비밀번호 발송
        emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), tempPassword);

        return user.getEmail();
    }

    /** 지정 길이의 임시 비밀번호 생성 — SecureRandom 사용으로 예측 불가 */
    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PW_LENGTH);
        for (int i = 0; i < TEMP_PW_LENGTH; i++) {
            sb.append(TEMP_PW_CHARS.charAt(SECURE_RANDOM.nextInt(TEMP_PW_CHARS.length())));
        }
        return sb.toString();
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

    /** 정렬순서 기준 전체 카테고리 조회 — AdminController 대시보드 렌더링용 */
    @Transactional(readOnly = true)
    public List<Category> findAllCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
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

    // ── 공지 조회 ──────────────────────────────────────────────

    /** 전체 공지 목록 — sortOrder 오름차순 (관리자 화면용, 숨김 포함) */
    @Transactional(readOnly = true)
    public List<Notice> findAllNotices() {
        return noticeRepository.findAllByOrderBySortOrderAsc();
    }

    /**
     * 활성 공지 목록 — 게시판 목록 상단 표시용
     * @param categoryId null = 전체 게시판 보기 (categories 비어있는 공지만)
     *                   값 있음 = categories 비어있는 공지 + 해당 카테고리 포함 공지
     */
    @Transactional(readOnly = true)
    public List<Notice> findActiveNotices(Long categoryId) {
        return categoryId == null
                ? noticeRepository.findByActiveTrueAndCategoriesEmpty()
                : noticeRepository.findActiveByCategoryId(categoryId);
    }

    // ── 공지 추가 ──────────────────────────────────────────────

    /**
     * 공지 추가 — 기존 최대 sortOrder + 1 로 맨 아래에 삽입
     * @param categoryIds 비어있거나 null = 전체 게시판 / 값 있음 = 선택된 카테고리에서만 표시
     */
    @Transactional
    public void addNotice(String title, String content, List<Long> categoryIds, String authorUsername) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("공지 제목을 입력해주세요.");
        }
        User author = findUser(authorUsername);
        Set<Category> categories = resolveCategories(categoryIds);

        int nextOrder = noticeRepository.findTopByOrderBySortOrderDesc()
                .map(n -> n.getSortOrder() + 1)
                .orElse(1);

        noticeRepository.save(Notice.builder()
                .author(author)
                .title(title.trim())
                .content(content)
                .categories(categories)
                .sortOrder(nextOrder)
                .build());
    }

    // ── 공지 수정 ──────────────────────────────────────────────

    @Transactional
    public void updateNotice(Long id, String title, String content, boolean active, List<Long> categoryIds) {
        Notice notice = findNotice(id);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("공지 제목을 입력해주세요.");
        }
        notice.setTitle(title.trim());
        notice.setContent(content);
        notice.setActive(active);
        // 기존 카테고리 교체 (clear 후 addAll — orphan 관리 불필요, 조인 테이블만 변경)
        notice.getCategories().clear();
        notice.getCategories().addAll(resolveCategories(categoryIds));
        noticeRepository.save(notice);
    }

    // ── 공지 삭제 ──────────────────────────────────────────────

    @Transactional
    public void deleteNotice(Long id) {
        noticeRepository.deleteById(id);
    }

    // ── 공지 순서 변경 ─────────────────────────────────────────

    /**
     * 공지를 한 단계 위로 이동 — 바로 위 공지와 sortOrder 를 교환
     */
    @Transactional
    public void moveNoticeUp(Long id) {
        Notice current = findNotice(id);
        noticeRepository
                .findTopBySortOrderLessThanOrderBySortOrderDesc(current.getSortOrder())
                .ifPresent(prev -> swapSortOrder(current, prev));
    }

    /**
     * 공지를 한 단계 아래로 이동 — 바로 아래 공지와 sortOrder 를 교환
     */
    @Transactional
    public void moveNoticeDown(Long id) {
        Notice current = findNotice(id);
        noticeRepository
                .findTopBySortOrderGreaterThanOrderBySortOrderAsc(current.getSortOrder())
                .ifPresent(next -> swapSortOrder(current, next));
    }

    private void swapSortOrder(Notice a, Notice b) {
        int tmp = a.getSortOrder();
        a.setSortOrder(b.getSortOrder());
        b.setSortOrder(tmp);
        noticeRepository.save(a);
        noticeRepository.save(b);
    }

    private Notice findNotice(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다."));
    }

    /** categoryIds → Category Set 변환 (null 또는 빈 리스트면 빈 Set 반환 = 전체 게시판) */
    private Set<Category> resolveCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return new HashSet<>();
        return categoryIds.stream()
                .map(cid -> categoryRepository.findById(cid)
                        .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + cid)))
                .collect(Collectors.toSet());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
    }

    // ── 커스텀 역할 조회 ───────────────────────────────────────

    /** 전체 커스텀 역할 목록 — 생성일 오름차순 */
    @Transactional(readOnly = true)
    public List<CustomRole> findAllCustomRoles() {
        return customRoleRepository.findAllByOrderByCreatedAtAsc();
    }

    /** 역할 ID → 할당 회원 수 맵 (대시보드 통계 표시용) */
    @Transactional(readOnly = true)
    public Map<Long, Long> getRoleUserCountMap() {
        Map<Long, Long> countMap = new HashMap<>();
        customRoleRepository.findAllByOrderByCreatedAtAsc()
                .forEach(cr -> countMap.put(cr.getId(),
                        userRepository.countByCustomRoles_Id(cr.getId())));
        return countMap;
    }

    /** 특정 커스텀 역할에 할당된 회원 목록 */
    @Transactional(readOnly = true)
    public List<User> getUsersByCustomRoleId(Long roleId) {
        if (!customRoleRepository.existsById(roleId)) {
            throw new IllegalArgumentException("역할을 찾을 수 없습니다.");
        }
        return userRepository.findByCustomRoles_Id(roleId);
    }

    /**
     * 특정 회원에서 특정 커스텀 역할만 해제
     * @param userId  대상 회원 ID
     * @param roleId  해제할 역할 ID
     */
    @Transactional
    public void unassignCustomRole(Long userId, Long roleId) {
        User user = findUser(userId);
        user.getCustomRoles().removeIf(r -> r.getId().equals(roleId));
        userRepository.save(user);
    }

    // ── 커스텀 역할 추가 ───────────────────────────────────────

    /**
     * 새 커스텀 역할 생성
     * @param name        역할명 (고유, 공백 불가)
     * @param description 역할 설명 (선택)
     * @param permissions 부여할 세부 기능 권한 목록
     */
    @Transactional
    public void addCustomRole(String name, String description, Set<Permission> permissions) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("역할명을 입력해주세요.");
        }
        if (customRoleRepository.existsByName(name.trim())) {
            throw new IllegalArgumentException("이미 존재하는 역할명입니다: " + name);
        }
        customRoleRepository.save(CustomRole.builder()
                .name(name.trim())
                .description(description != null ? description.trim() : null)
                .permissions(permissions != null ? permissions : new HashSet<>())
                .build());
    }

    // ── 커스텀 역할 권한 수정 ─────────────────────────────────

    /**
     * 기존 커스텀 역할의 세부 기능 권한 목록 교체
     * 역할명/설명도 함께 수정 가능
     */
    @Transactional
    public void updateCustomRole(Long roleId, String name, String description, Set<Permission> permissions) {
        CustomRole role = customRoleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("역할을 찾을 수 없습니다."));

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("역할명을 입력해주세요.");
        }
        // 이름 중복 체크 (자기 자신 제외)
        customRoleRepository.findByName(name.trim())
                .filter(r -> !r.getId().equals(roleId))
                .ifPresent(r -> { throw new IllegalArgumentException("이미 존재하는 역할명입니다: " + name); });

        role.setName(name.trim());
        role.setDescription(description != null ? description.trim() : null);
        // 기존 권한 교체
        role.getPermissions().clear();
        if (permissions != null) {
            role.getPermissions().addAll(permissions);
        }
        customRoleRepository.save(role);
    }

    // ── 커스텀 역할 삭제 ───────────────────────────────────────

    /**
     * 커스텀 역할 삭제
     * 해당 역할이 할당된 회원의 customRole 은 ON DELETE SET NULL 로 자동 null 처리됨
     */
    @Transactional
    public void deleteCustomRole(Long roleId) {
        if (!customRoleRepository.existsById(roleId)) {
            throw new IllegalArgumentException("역할을 찾을 수 없습니다.");
        }
        customRoleRepository.deleteById(roleId);
    }

    // ── 회원에게 커스텀 역할 할당/해제 (다대다) ──────────────────

    /**
     * 특정 회원의 커스텀 역할 목록을 교체 (기존 역할 전체 제거 후 새 역할 목록으로 설정)
     * @param userId  대상 회원 ID
     * @param roleIds 할당할 커스텀 역할 ID 집합 (빈 Set 이면 전체 해제)
     */
    @Transactional
    public void assignCustomRoles(Long userId, Set<Long> roleIds) {
        User user = findUser(userId);

        // 기존 역할 전체 제거
        user.getCustomRoles().clear();

        // 새 역할 목록 설정
        if (roleIds != null && !roleIds.isEmpty()) {
            Set<CustomRole> newRoles = roleIds.stream()
                    .map(id -> customRoleRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("역할을 찾을 수 없습니다: " + id)))
                    .collect(Collectors.toSet());
            user.getCustomRoles().addAll(newRoles);
        }

        userRepository.save(user);
    }
}