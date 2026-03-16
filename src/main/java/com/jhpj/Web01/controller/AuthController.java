package com.jhpj.Web01.controller;

import com.jhpj.Web01.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
                         @RequestParam String confirmPassword, // ✅ 추가
                         @RequestParam String email,
                         RedirectAttributes redirectAttributes) {
        try {
            userDetailsService.register(username, password, confirmPassword, email);
            redirectAttributes.addFlashAttribute("successMsg", "회원가입이 완료되었습니다. 로그인하세요.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/auth/signup";
        }
    }
}
