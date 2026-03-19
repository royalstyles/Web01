package com.jhpj.Web01.controller;

import com.jhpj.Web01.entity.EmailVerificationToken;
import com.jhpj.Web01.entity.User;
import com.jhpj.Web01.repository.EmailVerificationTokenRepository;
import com.jhpj.Web01.repository.UserRepository;
import com.jhpj.Web01.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomUserDetailsService userDetailsService;
    private final EmailVerificationTokenRepository tokenRepository;  // ✅ 추가
    private final UserRepository userRepository;                     // ✅ 추가

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

    /** 회원가입 처리 — baseUrl 추가 */
    @PostMapping("/signup")
    public String signup(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam String confirmPassword,
                         @RequestParam String email,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() == 80 || request.getServerPort() == 443
                    ? "" : ":" + request.getServerPort());
            userDetailsService.register(username, password, confirmPassword, email, baseUrl);
            redirectAttributes.addFlashAttribute("successMsg",
                    "인증 메일을 발송했습니다. 메일함을 확인해주세요.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/auth/signup";
        }
    }

    /** 이메일 인증 처리 */
    @GetMapping("/verify")
    @Transactional
    public String verifyEmail(@RequestParam String token, Model model) {
        Optional<EmailVerificationToken> opt = tokenRepository.findByToken(token);

        if (opt.isEmpty()) {
            model.addAttribute("msg", "유효하지 않은 인증 링크입니다.");
            model.addAttribute("success", false);
            return "auth/verify-result";
        }

        EmailVerificationToken evt = opt.get();
        if (evt.isExpired()) {
            model.addAttribute("msg", "인증 링크가 만료되었습니다. 다시 회원가입해주세요.");
            model.addAttribute("success", false);
            return "auth/verify-result";
        }

        // 인증 완료 처리
        User user = evt.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.delete(evt);

        model.addAttribute("msg", "이메일 인증이 완료되었습니다! 로그인하세요.");
        model.addAttribute("success", true);
        return "auth/verify-result";
    }
}
