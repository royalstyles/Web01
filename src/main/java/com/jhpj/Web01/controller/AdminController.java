package com.jhpj.Web01.controller;

import com.jhpj.Web01.entity.User;
import com.jhpj.Web01.repository.UserRepository;
import com.jhpj.Web01.service.AdminService;
import com.jhpj.Web01.service.LoginAttemptService;
import com.jhpj.Web01.service.QuasarZoneImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 관리자 대시보드 컨트롤러 — /admin/** 경로 처리
 * ROLE_ADMIN 권한이 있는 사용자만 접근 가능 (SecurityConfig 에서 hasRole("ADMIN") 설정)
 * 회원 목록/통계 표시, 권한 변경, 회원 삭제, 계정 잠금 해제, 이메일 인증 강제 완료 처리
 * 게시판 카테고리 추가/수정/삭제도 이 컨트롤러에서 담당
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final AdminService adminService;
    private final LoginAttemptService loginAttemptService;
    private final QuasarZoneImportService importService;

    /** 대시보드 */
    @GetMapping
    public String dashboard(Model model,
                            @AuthenticationPrincipal UserDetails currentUser) {

        List<User> users = userRepository.findAll();

        // ── 통계 ──────────────────────────────────────────
        long totalUsers     = users.size();
        long adminCount     = users.stream().filter(u -> u.getRole() == User.Role.ROLE_ADMIN).count();
        long unverifiedCount= users.stream().filter(u -> !u.isEmailVerified()).count();
        long lockedCount    = users.stream().filter(u -> loginAttemptService.isBlocked(u.getUsername())).count();

        // ── 잠금된 userId Set (Thymeleaf 조건 렌더링용) ──
        Set<Long> lockedIds = users.stream()
                .filter(u -> loginAttemptService.isBlocked(u.getUsername()))
                .map(User::getId)
                .collect(Collectors.toSet());

        model.addAttribute("users",           users);
        model.addAttribute("totalUsers",      totalUsers);
        model.addAttribute("adminCount",      adminCount);
        model.addAttribute("unverifiedCount", unverifiedCount);
        model.addAttribute("lockedCount",     lockedCount);
        model.addAttribute("lockedIds",       lockedIds);
        model.addAttribute("currentUsername", currentUser.getUsername());
        model.addAttribute("categories",       adminService.findAllCategories());
        model.addAttribute("notices",          adminService.findAllNotices());
        model.addAttribute("importLastResult", importService.getLastRunResult());
        model.addAttribute("importLastRunAt",  importService.getLastRunAt());

        return "admin";
    }

    /** 권한 변경 */
    @PostMapping("/users/{id}/role")
    public String toggleRole(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails currentUser,
                             RedirectAttributes ra) {
        guardSelf(id, currentUser, ra, "자신의 권한은 변경할 수 없습니다.");
        if (ra.getFlashAttributes().containsKey("errorMsg")) return "redirect:/admin";

        adminService.toggleRole(id);
        ra.addFlashAttribute("successMsg", "권한이 변경되었습니다.");
        return "redirect:/admin";
    }

    /** 회원 삭제 */
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails currentUser,
                             RedirectAttributes ra) {
        guardSelf(id, currentUser, ra, "자신의 계정은 삭제할 수 없습니다.");
        if (ra.getFlashAttributes().containsKey("errorMsg")) return "redirect:/admin";

        adminService.deleteUser(id);
        ra.addFlashAttribute("successMsg", "회원이 삭제되었습니다.");
        return "redirect:/admin";
    }

    /** 계정 잠금 해제 */
    @PostMapping("/users/{id}/unlock")
    public String unlockUser(@PathVariable Long id, RedirectAttributes ra) {
        adminService.unlockUser(id);
        ra.addFlashAttribute("successMsg", "계정 잠금이 해제되었습니다.");
        return "redirect:/admin";
    }

    /** 이메일 인증 강제 완료 */
    @PostMapping("/users/{id}/verify")
    public String forceVerify(@PathVariable Long id, RedirectAttributes ra) {
        adminService.forceVerify(id);
        ra.addFlashAttribute("successMsg", "이메일 인증이 완료 처리되었습니다.");
        return "redirect:/admin";
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────
    private void guardSelf(Long targetId, UserDetails currentUser,
                           RedirectAttributes ra, String msg) {
        userRepository.findByUsername(currentUser.getUsername())
                .ifPresent(me -> {
                    if (me.getId().equals(targetId)) {
                        ra.addFlashAttribute("errorMsg", msg);
                    }
                });
    }

    // ── 퀘이사존 수집 트리거 ──────────────────────────────────────

    /** 관리자 화면에서 수동으로 퀘이사존 수집 실행 */
    @PostMapping("/import/quasarzone")
    public String triggerQuasarZoneImport(
            @RequestParam(defaultValue = "1") int pages,
            RedirectAttributes ra) {
        String result = importService.runImport(Math.min(pages, 5)); // 최대 5페이지 제한
        ra.addFlashAttribute("successMsg", "[퀘이사존 수집] " + result);
        return "redirect:/admin";
    }

    // ── 공지 추가 ──────────────────────────────────────────────
    @PostMapping("/notices/add")
    public String addNotice(@RequestParam String title,
                            @RequestParam(required = false) String content,
                            @RequestParam(required = false) List<Long> categoryIds,
                            @AuthenticationPrincipal UserDetails currentUser,
                            RedirectAttributes ra) {
        try {
            adminService.addNotice(title, content, categoryIds, currentUser.getUsername());
            ra.addFlashAttribute("successMsg", "공지가 등록되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin#notices";
    }

    // ── 공지 수정 ──────────────────────────────────────────────
    @PostMapping("/notices/{id}/update")
    public String updateNotice(@PathVariable Long id,
                               @RequestParam String title,
                               @RequestParam(required = false) String content,
                               @RequestParam(defaultValue = "false") boolean active,
                               @RequestParam(required = false) List<Long> categoryIds,
                               RedirectAttributes ra) {
        try {
            adminService.updateNotice(id, title, content, active, categoryIds);
            ra.addFlashAttribute("successMsg", "공지가 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin#notices";
    }

    // ── 공지 삭제 ──────────────────────────────────────────────
    @PostMapping("/notices/{id}/delete")
    public String deleteNotice(@PathVariable Long id, RedirectAttributes ra) {
        adminService.deleteNotice(id);
        ra.addFlashAttribute("successMsg", "공지가 삭제되었습니다.");
        return "redirect:/admin#notices";
    }

    // ── 공지 순서 위로 ─────────────────────────────────────────
    @PostMapping("/notices/{id}/move-up")
    public String moveNoticeUp(@PathVariable Long id, RedirectAttributes ra) {
        adminService.moveNoticeUp(id);
        return "redirect:/admin#notices";
    }

    // ── 공지 순서 아래로 ───────────────────────────────────────
    @PostMapping("/notices/{id}/move-down")
    public String moveNoticeDown(@PathVariable Long id, RedirectAttributes ra) {
        adminService.moveNoticeDown(id);
        return "redirect:/admin#notices";
    }

    // ── 카테고리 추가 ──────────────────────────────────────────
    @PostMapping("/categories/add")
    public String addCategory(@RequestParam String name,
                              @RequestParam(defaultValue = "0") int sortOrder,
                              RedirectAttributes ra) {
        try {
            adminService.addCategory(name, sortOrder);
            ra.addFlashAttribute("successMsg", "카테고리가 추가되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin";
    }

    // ── 카테고리 수정 ──────────────────────────────────────────
    @PostMapping("/categories/{id}/update")
    public String updateCategory(@PathVariable Long id,
                                 @RequestParam String name,
                                 @RequestParam(defaultValue = "0") int sortOrder,
                                 RedirectAttributes ra) {
        try {
            adminService.updateCategory(id, name, sortOrder);
            ra.addFlashAttribute("successMsg", "카테고리가 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin";
    }

    // ── 카테고리 삭제 ──────────────────────────────────────────
    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.deleteCategory(id);
            ra.addFlashAttribute("successMsg", "카테고리가 삭제되었습니다. 해당 게시글은 미분류로 유지됩니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin";
    }
}