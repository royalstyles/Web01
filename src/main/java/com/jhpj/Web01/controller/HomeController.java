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

/**
 * 홈/메인 페이지 컨트롤러
 * 루트 경로(/) 를 /home 으로 리다이렉트하고, /home 에서 게시글 목록을 표시
 * 로그인/비로그인 사용자 모두 접근 가능 (SecurityConfig 에서 permitAll 설정)
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserRepository userRepository;
    private final BoardService boardService;

    /** 루트 경로 접근 시 /home 으로 리다이렉트 */
    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    /**
     * 홈 화면 — 카테고리 필터, 키워드 검색, 페이징을 지원하는 게시글 목록
     * 로그인 여부에 따라 헤더 표시 내용이 달라지므로 userDetails 가 null 일 수 있음
     */
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserDetails userDetails,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        // ── 로그인 여부에 따라 분기 ──────────────────────────
        if (userDetails != null) {
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("username", userDetails.getUsername());
            model.addAttribute("isAdmin",
                    userDetails.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

            // 프로필 이미지
            userRepository.findByUsername(userDetails.getUsername())
                    .ifPresent(u -> model.addAttribute("profileImage", u.getProfileImage()));
        } else {
            model.addAttribute("isLoggedIn", false);
        }

        // ── 게시판 데이터 (공통) ─────────────────────────────
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