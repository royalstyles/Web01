package com.jhpj.Web01.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 홈/루트 리다이렉트 컨트롤러
 * /  및 /home 접근 시 /board 로 리다이렉트
 * 게시글 목록은 BoardController(/board)에서 단일 관리
 */
@Controller
public class HomeController {

    /** 루트 경로 → /board 리다이렉트 */
    @GetMapping("/")
    public String root() {
        return "redirect:/board";
    }

    /**
     * /home → /board 리다이렉트 (쿼리 파라미터 유지)
     * 기존 북마크나 링크 호환성을 위해 유지
     */
    @GetMapping("/home")
    public String home(HttpServletRequest request) {
        // categoryId, keyword, page 등 기존 파라미터를 그대로 /board 로 전달
        String query = request.getQueryString();
        return "redirect:/board" + (query != null ? "?" + query : "");
    }
}
