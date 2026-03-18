package com.jhpj.Web01.controller;

import com.jhpj.Web01.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /** 프로필 페이지 */
    @GetMapping
    public String profilePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
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
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() == 80 || request.getServerPort() == 443
                    ? "" : ":" + request.getServerPort());
            profileService.requestEmailChange(userDetails.getUsername(), newEmail, baseUrl);
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