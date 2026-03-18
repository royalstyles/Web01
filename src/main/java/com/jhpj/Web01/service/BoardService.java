package com.jhpj.Web01.service;

import com.jhpj.Web01.entity.*;
import com.jhpj.Web01.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    // Quill.js 본문 HTML에서 storedName 추출용 패턴
    // /uploads/images/uuid.jpg  또는  /uploads/videos/uuid.mp4
    private static final Pattern FILE_URL_PATTERN =
            Pattern.compile("/uploads/(?:images|videos)/([^\"'\\s]+)");

    // ── 카테고리 ─────────────────────────────────────────────

    public List<Category> findAllCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }

    // ── 게시글 목록 ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Post> getPostList(Long categoryId, String keyword, int page) {
        Pageable pageable = PageRequest.of(page, 10);  // 정렬은 쿼리에 포함

        boolean hasCategory = categoryId != null;
        boolean hasKeyword  = keyword != null && !keyword.isBlank();

        if (hasCategory && hasKeyword) {
            return postRepository.findByCategoryAndKeywordWithDetails(categoryId, keyword, pageable);
        } else if (hasCategory) {
            return postRepository.findByCategoryWithDetails(categoryId, pageable);
        } else if (hasKeyword) {
            return postRepository.findByKeywordWithDetails(keyword, pageable);
        } else {
            return postRepository.findAllWithDetails(pageable);
        }
    }

    // ── 게시글 상세 ──────────────────────────────────────────

    @Transactional
    public Post getPost(Long postId) {
        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        post.increaseViewCount();
        return post;
    }

    @Transactional(readOnly = true)
    public Post getPostReadOnly(Long postId) {
        return postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    }

    // ── 게시글 등록 ──────────────────────────────────────────

    @Transactional
    public Post createPost(String username, Long categoryId,
                           String title, String content) {
        User author = findUser(username);
        Category category = (categoryId != null)
                ? categoryRepository.findById(categoryId).orElse(null)
                : null;

        Post post = Post.builder()
                .author(author)
                .category(category)
                .title(title)
                .content(content)
                .build();

        Post saved = postRepository.save(post);

        // 본문 내 업로드 파일 연결 (임시 → 정식)
        List<String> storedNames = extractStoredNames(content);
        fileService.attachFilesToPost(saved, storedNames);

        return saved;
    }

    // ── 게시글 수정 ──────────────────────────────────────────

    @Transactional
    public void updatePost(Long postId, String username, Long categoryId,
                           String title, String content) {
        Post post = getPostReadOnly(postId);
        checkAuthor(post, username);

        Category category = (categoryId != null)
                ? categoryRepository.findById(categoryId).orElse(null)
                : null;

        post.setTitle(title);
        post.setContent(content);
        post.setCategory(category);

        // 새로 추가된 파일 연결
        List<String> storedNames = extractStoredNames(content);
        fileService.attachFilesToPost(post, storedNames);

        postRepository.save(post);
    }

    // ── 게시글 삭제 ──────────────────────────────────────────

    @Transactional
    public void deletePost(Long postId, String username) {
        Post post = getPostReadOnly(postId);
        checkAuthor(post, username);

        // 첨부파일 디스크 삭제 (DB는 CASCADE)
        post.getFiles().forEach(f -> fileService.delete(f.getStoredName()));

        postRepository.delete(post);
    }

    // ── 댓글 ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Comment> getComments(Long postId) {
        return commentRepository.findByPostIdWithAuthor(postId);
    }

    @Transactional
    public Comment addComment(Long postId, String username, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("댓글 내용을 입력해주세요.");
        }
        Post post = getPostReadOnly(postId);
        User author = findUser(username);

        return commentRepository.save(Comment.builder()
                .post(post)
                .author(author)
                .content(content)
                .build());
    }

    @Transactional
    public void deleteComment(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 본인 또는 관리자만 삭제 가능
        boolean isAdmin = findUser(username).getRole() == User.Role.ROLE_ADMIN;
        if (!comment.getAuthor().getUsername().equals(username) && !isAdmin) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        commentRepository.delete(comment);
    }

    @Transactional
    public void updateComment(Long commentId, String username, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getAuthor().getUsername().equals(username)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }
        comment.setContent(content);
        commentRepository.save(comment);
    }

    // ── 좋아요 토글 ──────────────────────────────────────────

    @Transactional
    public boolean toggleLike(Long postId, String username) {
        boolean already = postLikeRepository
                .existsByPostIdAndUserUsername(postId, username);

        if (already) {
            // 좋아요 취소
            postLikeRepository.findByPostIdAndUserUsername(postId, username)
                    .ifPresent(postLikeRepository::delete);
            return false;
        } else {
            // 좋아요 추가
            Post post = getPostReadOnly(postId);
            User user = findUser(username);
            postLikeRepository.save(PostLike.builder()
                    .post(post)
                    .user(user)
                    .build());
            return true;
        }
    }

    /** 현재 사용자가 좋아요 눌렀는지 확인 */
    @Transactional(readOnly = true)
    public boolean isLiked(Long postId, String username) {
        return postLikeRepository.existsByPostIdAndUserUsername(postId, username);
    }

    /** 좋아요 수 조회 (트리거로 POSTS.LIKE_COUNT 동기화되므로 Post 객체에서 바로 읽어도 됨) */
    @Transactional(readOnly = true)
    public long getLikeCount(Long postId) {
        return postLikeRepository.countByPostId(postId);
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private void checkAuthor(Post post, String username) {
        boolean isAdmin = findUser(username).getRole() == User.Role.ROLE_ADMIN;
        if (!post.getAuthor().getUsername().equals(username) && !isAdmin) {
            throw new IllegalStateException("수정/삭제 권한이 없습니다.");
        }
    }

    /**
     * Quill.js 본문 HTML에서 storedName 목록 추출
     * 예) /uploads/images/abc123.jpg → "abc123.jpg"
     */
    private List<String> extractStoredNames(String content) {
        if (content == null || content.isBlank()) return List.of();
        Matcher matcher = FILE_URL_PATTERN.matcher(content);
        List<String> names = new java.util.ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }
}