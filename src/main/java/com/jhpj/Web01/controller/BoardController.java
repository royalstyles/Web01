package com.jhpj.Web01.controller;

import com.jhpj.Web01.entity.Category;
import com.jhpj.Web01.entity.Comment;
import com.jhpj.Web01.entity.Post;
import com.jhpj.Web01.repository.UserRepository;
import com.jhpj.Web01.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * 게시판 컨트롤러 — /board/** 경로 처리
 * 게시글 목록/상세/작성/수정/삭제 처리 (동기 방식)
 * 댓글 등록/수정/삭제 및 좋아요 토글은 AJAX 요청으로 처리 (ResponseEntity 반환)
 * 비로그인 사용자는 목록/상세 조회만 가능하며, 나머지는 SecurityConfig 에서 인증 필요로 설정
 */
@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final UserRepository userRepository;

    // ── 목록 ─────────────────────────────────────────────────

    @GetMapping
    public String list(@RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String searchType,
                       @RequestParam(defaultValue = "0") int page,
                       @AuthenticationPrincipal UserDetails userDetails,
                       Model model) {

        // ── 헤더 프래그먼트용 로그인 정보 주입 ──
        addHeaderAttributes(userDetails, model);

        Page<Post> postPage = boardService.getPostList(categoryId, keyword, searchType, page);

        // findAllCategories() 를 한 번만 호출해 목록 조회와 카테고리명 추출에 재사용
        List<Category> categories = boardService.findAllCategories();
        String boardName = (categoryId == null) ? "게시판"
                : categories.stream()
                        .filter(c -> c.getId().equals(categoryId))
                        .findFirst()
                        .map(Category::getName)
                        .orElse("게시판");

        model.addAttribute("posts",            postPage.getContent());
        model.addAttribute("postPage",         postPage);
        model.addAttribute("categories",       categories);
        model.addAttribute("categoryId",       categoryId);
        model.addAttribute("keyword",          keyword);
        model.addAttribute("searchType",       searchType);
        model.addAttribute("currentPage",      page);
        model.addAttribute("currentBoardName", boardName);

        return "board/board-list";
    }

    // ── 작성 폼 ───────────────────────────────────────────────

    @GetMapping("/write")
    public String writeForm(@AuthenticationPrincipal UserDetails userDetails,
                            Model model) {
        addHeaderAttributes(userDetails, model);
        model.addAttribute("categories", boardService.findAllCategories());
        return "board/board-write";
    }

    // ── 작성 처리 ─────────────────────────────────────────────

    @PostMapping("/write")
    public String write(@RequestParam(required = false) Long categoryId,
                        @RequestParam String title,
                        @RequestParam String content,
                        @AuthenticationPrincipal UserDetails userDetails,
                        RedirectAttributes ra) {
        try {
            Post post = boardService.createPost(
                    userDetails.getUsername(), categoryId, title, content);
            ra.addFlashAttribute("successMsg", "게시글이 등록되었습니다.");
            return "redirect:/board/" + post.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/board/write";
        }
    }

    // ── 상세 ─────────────────────────────────────────────────

    @GetMapping("/{postId}")
    public String view(@PathVariable Long postId,
                       @AuthenticationPrincipal UserDetails userDetails,
                       Model model) {

        addHeaderAttributes(userDetails, model);

        Post post = boardService.getPost(postId);
        List<Comment> comments = boardService.getComments(postId);

        model.addAttribute("post",     post);
        model.addAttribute("comments", comments);

        if (userDetails != null) {
            boolean liked = boardService.isLiked(postId, userDetails.getUsername());
            model.addAttribute("liked",    liked);
            model.addAttribute("currentUsername", userDetails.getUsername());

            boolean isAuthor = post.getAuthor().getUsername().equals(userDetails.getUsername());
            boolean isAdmin  = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            model.addAttribute("canEdit", isAuthor || isAdmin);
        } else {
            model.addAttribute("liked",           false);
            model.addAttribute("currentUsername", "");
            model.addAttribute("canEdit",         false);
        }

        return "board/board-view";
    }

    // ── 수정 폼 ───────────────────────────────────────────────

    @GetMapping("/{postId}/edit")
    public String editForm(@PathVariable Long postId,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model,
                           RedirectAttributes ra) {

        addHeaderAttributes(userDetails, model); // ← 추가

        Post post = boardService.getPostReadOnly(postId);

        boolean isAuthor = post.getAuthor().getUsername().equals(userDetails.getUsername());
        boolean isAdmin  = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAuthor && !isAdmin) {
            ra.addFlashAttribute("errorMsg", "수정 권한이 없습니다.");
            return "redirect:/board/" + postId;
        }

        model.addAttribute("post",       post);
        model.addAttribute("categories", boardService.findAllCategories());
        return "board/board-write";     // 작성 폼 재사용 (post 있으면 수정 모드)
    }

    // ── 수정 처리 ─────────────────────────────────────────────

    @PostMapping("/{postId}/edit")
    public String edit(@PathVariable Long postId,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam String title,
                       @RequestParam String content,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes ra) {
        try {
            boardService.updatePost(postId, userDetails.getUsername(),
                    categoryId, title, content);
            ra.addFlashAttribute("successMsg", "게시글이 수정되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/board/" + postId;
    }

    // ── 삭제 처리 ─────────────────────────────────────────────

    @PostMapping("/{postId}/delete")
    public String delete(@PathVariable Long postId,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes ra) {
        try {
            boardService.deletePost(postId, userDetails.getUsername());
            ra.addFlashAttribute("successMsg", "게시글이 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/board";
    }

    // ── 댓글 등록 (AJAX) ─────────────────────────────────────

    @PostMapping("/{postId}/comments")
    @ResponseBody
    public ResponseEntity<?> addComment(@PathVariable Long postId,
                                        @RequestParam String content,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Comment comment = boardService.addComment(
                    postId, userDetails.getUsername(), content);
            return ResponseEntity.ok(Map.of(
                    "id",        comment.getId(),
                    "content",   comment.getContent(),
                    "author",    comment.getAuthor().getUsername(),
                    "createdAt", comment.getCreatedAt().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── 댓글 수정 (AJAX) ─────────────────────────────────────

    @PutMapping("/comments/{commentId}")
    @ResponseBody
    public ResponseEntity<?> updateComment(@PathVariable Long commentId,
                                           @RequestParam String content,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        try {
            boardService.updateComment(commentId, userDetails.getUsername(), content);
            return ResponseEntity.ok(Map.of("message", "수정 완료"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── 댓글 삭제 (AJAX) ─────────────────────────────────────

    @DeleteMapping("/comments/{commentId}")
    @ResponseBody
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        try {
            boardService.deleteComment(commentId, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("message", "삭제 완료"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── 좋아요 토글 (AJAX) ───────────────────────────────────

    @PostMapping("/{postId}/like")
    @ResponseBody
    public ResponseEntity<?> toggleLike(@PathVariable Long postId,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        try {
            boolean liked = boardService.toggleLike(postId, userDetails.getUsername());
            long count    = boardService.getLikeCount(postId);
            return ResponseEntity.ok(Map.of("liked", liked, "count", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── 헤더 프래그먼트용 로그인 정보 공통 주입 ──────────────
    private void addHeaderAttributes(UserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("username", userDetails.getUsername());
            model.addAttribute("isAdmin",
                    userDetails.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
            userRepository.findByUsername(userDetails.getUsername())
                    .ifPresent(u -> model.addAttribute("profileImage", u.getProfileImage()));
        } else {
            model.addAttribute("isLoggedIn", false);
        }
    }
}