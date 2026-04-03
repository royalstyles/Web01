package com.jhpj.Web01.controller;

import com.jhpj.Web01.entity.Post;
import com.jhpj.Web01.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/my-posts")
@RequiredArgsConstructor
public class MyPostsController {

    private final BoardService boardService;

    /** 내가 쓴 글 목록 */
    @GetMapping
    public String myPosts(@AuthenticationPrincipal UserDetails userDetails,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        String username = userDetails.getUsername();

        // 내 글 목록 (페이징)
        Page<Post> postPage = boardService.getMyPosts(username, page);
        model.addAttribute("postPage", postPage);
        model.addAttribute("posts", postPage.getContent());
        model.addAttribute("currentPage", page);

        return "my-posts";
    }

    /** 내 글 삭제 (목록에서 바로 삭제) */
    @PostMapping("/{postId}/delete")
    public String deleteMyPost(@AuthenticationPrincipal UserDetails userDetails,
                               @PathVariable Long postId,
                               @RequestParam(defaultValue = "0") int page,
                               RedirectAttributes ra) {
        try {
            boardService.deletePost(postId, userDetails.getUsername());
            ra.addFlashAttribute("successMsg", "게시글이 삭제되었습니다.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/my-posts?page=" + page;
    }
}
