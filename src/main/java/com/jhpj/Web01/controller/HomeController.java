package com.jhpj.Web01.controller;

import com.jhpj.Web01.entity.User;
import com.jhpj.Web01.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserRepository userRepository;

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("username", userDetails.getUsername());
        model.addAttribute("isAdmin",
                userDetails.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        return "home";
    }

    /** 관리자: 전체 회원 목록 */
    @GetMapping("/admin")
    public String admin(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin";
    }
}
