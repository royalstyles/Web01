package com.jhpj.Web01.controller;

import com.jhpj.Web01.entity.Post;
import com.jhpj.Web01.repository.UserRepository;
import com.jhpj.Web01.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserRepository userRepository;
    private final BoardService boardService;

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserDetails userDetails,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        model.addAttribute("username", userDetails.getUsername());
        model.addAttribute("isAdmin",
                userDetails.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        // 프로필 이미지
        userRepository.findByUsername(userDetails.getUsername())
                .ifPresent(u -> model.addAttribute("profileImage", u.getProfileImage()));

        // 게시판 데이터
        Page<Post> postPage = boardService.getPostList(categoryId, keyword, page);
        model.addAttribute("posts",       postPage.getContent());
        model.addAttribute("postPage",    postPage);
        model.addAttribute("categories",  boardService.findAllCategories());
        model.addAttribute("categoryId",  categoryId);
        model.addAttribute("keyword",     keyword);
        model.addAttribute("currentPage", page);

        // 현재 카테고리명 (네비게이션 표시용)
        if (categoryId != null) {
            boardService.findAllCategories().stream()
                    .filter(c -> c.getId().equals(categoryId))
                    .findFirst()
                    .ifPresent(c -> model.addAttribute("currentCategoryName", c.getName()));
        }

        return "home";
    }
}