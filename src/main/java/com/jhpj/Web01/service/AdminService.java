package com.jhpj.Web01.service;

import com.jhpj.Web01.entity.Category;
import com.jhpj.Web01.entity.Notice;
import com.jhpj.Web01.entity.User;
import com.jhpj.Web01.repository.CategoryRepository;
import com.jhpj.Web01.repository.EmailVerificationTokenRepository;
import com.jhpj.Web01.repository.NoticeRepository;
import com.jhpj.Web01.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
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
}