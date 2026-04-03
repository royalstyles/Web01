package com.jhpj.Web01.controller;

import com.jhpj.Web01.service.ProfileService;
import com.jhpj.Web01.util.WebUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 프로필 관리 컨트롤러 — /profile/** 경로 처리
 * 로그인한 사용자 본인의 프로필 이미지/아이디/비밀번호/이메일 변경을 처리
 * 모든 /profile/** 경로는 SecurityConfig 에서 인증 필수로 설정
 * 아이디 변경 시 세션 무효화 후 재로그인 유도, 이메일 변경 시 인증 메일 발송
 *
 * [본인 확인] 프로필 페이지 접근 전 비밀번호 재확인 필수
 *   - 세션 속성 PROFILE_VERIFIED_AT 에 검증 시각(ms) 저장
 *   - 유효 시간: 10분 (VERIFY_VALID_MS)
 */
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /** 세션에서 사용할 속성명 및 유효 시간 (10분) */
    private static final String SESSION_KEY   = "PROFILE_VERIFIED_AT";
    private static final long   VERIFY_VALID_MS = 10 * 60 * 1000L;

    /**
     * 세션에 유효한 본인 확인 기록이 있는지 검사
     * @return true: 10분 이내 인증 완료 상태
     */
    private boolean isVerified(HttpSession session) {
        Long verifiedAt = (Long) session.getAttribute(SESSION_KEY);
        return verifiedAt != null
                && (System.currentTimeMillis() - verifiedAt) < VERIFY_VALID_MS;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 본인 확인 (비밀번호 재입력)
    // ──────────────────────────────────────────────────────────────────────────

    /** 본인 확인 폼 표시 */
    @GetMapping("/verify")
    public String verifyForm(@AuthenticationPrincipal UserDetails userDetails,
                             HttpSession session,
                             Model model) {
        // 이미 인증된 상태라면 바로 프로필로 이동
        if (isVerified(session)) {
            return "redirect:/profile";
        }
        return "profile-verify";
    }

    /** 본인 확인 처리 — 비밀번호 일치 시 세션에 타임스탬프 저장 */
    @PostMapping("/verify")
    public String verifySubmit(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam String password,
                               HttpSession session,
                               Model model) {
        if (profileService.verifyPassword(userDetails.getUsername(), password)) {
            // 인증 성공 — 현재 시각 저장
            session.setAttribute(SESSION_KEY, System.currentTimeMillis());
            return "redirect:/profile";
        }
        // 인증 실패 — 오류 메시지와 함께 폼 재표시
        model.addAttribute("errorMsg", "비밀번호가 올바르지 않습니다.");
        return "profile-verify";
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 프로필 페이지
    // ──────────────────────────────────────────────────────────────────────────

    /** 프로필 페이지 — 본인 확인이 안 된 경우 verify 페이지로 리다이렉트 */
    @GetMapping
    public String profilePage(@AuthenticationPrincipal UserDetails userDetails,
                              HttpSession session,
                              Model model) {
        if (!isVerified(session)) {
            return "redirect:/profile/verify";
        }
        model.addAttribute("user", profileService.findByUsername(userDetails.getUsername()));
        return "profile";
    }

    /** 프로필 이미지 업로드 */
    @PostMapping("/image")
    public String updateProfileImage(@AuthenticationPrincipal UserDetails userDetails,
                                     @RequestParam MultipartFile profileImage,
                                     RedirectAttributes ra) {
        try {
            profileService.updateProfileImage(userDetails.getUsername(), profileImage);
            ra.addFlashAttribute("successMsg", "프로필 이미지가 변경되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/profile";
    }

    /** 프로필 이미지 삭제 */
    @PostMapping("/image/delete")
    public String deleteProfileImage(@AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes ra) {
        try {
            profileService.deleteProfileImage(userDetails.getUsername());
            ra.addFlashAttribute("successMsg", "프로필 이미지가 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/profile";
    }

    /** 닉네임(아이디) 변경 */
    @PostMapping("/username")
    public String changeUsername(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam String newUsername,
                                 RedirectAttributes ra) {
        try {
            profileService.changeUsername(userDetails.getUsername(), newUsername);
            ra.addFlashAttribute("successMsg", "아이디가 변경되었습니다. 다시 로그인해주세요.");
            // username 변경 시 세션 무효화 → 재로그인 유도
            return "redirect:/auth/logout";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/profile";
        }
    }

    /** 비밀번호 변경 */
    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmNewPassword,
                                 RedirectAttributes ra) {
        try {
            profileService.changePassword(userDetails.getUsername(),
                    currentPassword, newPassword, confirmNewPassword);
            ra.addFlashAttribute("successMsg", "비밀번호가 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/profile";
    }

    /** 이메일 변경 요청 → 새 이메일로 인증 메일 발송 */
    @PostMapping("/email")
    public String changeEmail(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam String newEmail,
                              HttpServletRequest request,
                              RedirectAttributes ra) {
        try {
            profileService.requestEmailChange(userDetails.getUsername(), newEmail,
                    WebUtils.extractBaseUrl(request));
            ra.addFlashAttribute("successMsg",
                    "'" + newEmail + "' 으로 인증 메일을 발송했습니다. 인증 완료 후 이메일이 변경됩니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/profile";
    }

    /** 이메일 변경 인증 링크 처리 */
    @GetMapping("/email/verify")
    public String verifyEmailChange(@RequestParam String token, Model model) {
        try {
            String newEmail = profileService.confirmEmailChange(token);
            model.addAttribute("msg", "이메일이 '" + newEmail + "' 으로 변경되었습니다.");
            model.addAttribute("success", true);
        } catch (IllegalStateException e) {
            model.addAttribute("msg", e.getMessage());
            model.addAttribute("success", false);
        }
        return "profile-verify-result";
    }
}