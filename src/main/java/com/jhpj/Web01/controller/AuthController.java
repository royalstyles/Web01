package com.jhpj.Web01.controller;

import com.jhpj.Web01.service.CustomUserDetailsService;
import com.jhpj.Web01.util.WebUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 인증 관련 컨트롤러 — 로그인/회원가입/이메일 인증 처리
 * 로그인 처리 자체는 Spring Security 가 담당하며, 여기서는 UI 렌더링과 회원가입/이메일 인증만 처리
 * 모든 /auth/** 경로는 SecurityConfig 에서 permitAll 로 공개 설정
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomUserDetailsService userDetailsService;

    /** 로그인 페이지 */
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null)  model.addAttribute("errorMsg", "아이디 또는 비밀번호가 올바르지 않습니다.");
        if (logout != null) model.addAttribute("logoutMsg", "로그아웃 되었습니다.");
        return "auth/login";
    }

    /** 회원가입 페이지 */
    @GetMapping("/signup")
    public String signupPage() {
        return "auth/signup";
    }

    /** 회원가입 처리 */
    @PostMapping("/signup")
    public String signup(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam String confirmPassword,
                         @RequestParam String email,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        try {
            userDetailsService.register(username, password, confirmPassword, email,
                    WebUtils.extractBaseUrl(request));
            redirectAttributes.addFlashAttribute("successMsg",
                    "인증 메일을 발송했습니다. 메일함을 확인해주세요.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/auth/signup";
        }
    }

    /** 이메일 인증 처리 — 실제 처리는 CustomUserDetailsService.verifyEmail() 위임 */
    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String token, Model model) {
        try {
            userDetailsService.verifyEmail(token);
            model.addAttribute("msg", "이메일 인증이 완료되었습니다! 로그인하세요.");
            model.addAttribute("success", true);
        } catch (IllegalArgumentException e) {
            model.addAttribute("msg", e.getMessage());
            model.addAttribute("success", false);
        }
        return "auth/verify-result";
    }
}
