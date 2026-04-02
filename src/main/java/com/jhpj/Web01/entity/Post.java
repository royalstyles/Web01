package com.jhpj.Web01.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 게시글 엔티티 — POSTS 테이블 매핑
 * 작성자(User), 카테고리(Category), 댓글/파일/좋아요 등 여러 연관관계의 집계 루트 역할
 * content 는 Quill.js 에디터가 생성한 HTML 형태로 저장 (Oracle CLOB 타입)
 */
@Entity
@Table(name = "POSTS")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Post {

    /** PK — Oracle POSTS_SEQ 시퀀스로 자동 생성 */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "posts_seq")
    @SequenceGenerator(name = "posts_seq", sequenceName = "POSTS_SEQ", allocationSize = 1)
    private Long id;

    /** 게시글 작성자 — LAZY 로딩, 목록 조회 시 FETCH JOIN 으로 N+1 방지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    /** 게시글 카테고리 — null 허용 (카테고리 삭제 시 SET NULL) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /** 게시글 제목 (최대 200자) */
    @Column(nullable = false, length = 200)
    private String title;

    /** Quill.js 에디터에서 생성된 HTML 본문 — Oracle CLOB 타입으로 대용량 허용 */
    @Lob
    @Column(columnDefinition = "CLOB")
    private String content;

    /** 조회수 — 게시글 상세 조회 시 increaseViewCount() 로 증가 */
    @Column(nullable = false)
    @Builder.Default
    private int viewCount = 0;

    /** 좋아요 수 — Oracle 트리거로 POST_LIKES 변경 시 자동 동기화 */
    @Column(nullable = false)
    @Builder.Default
    private int likeCount = 0;

    /** 최초 작성 일시 — 수정 불가 */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 마지막 수정 일시 — @PreUpdate 로 자동 갱신 */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** 댓글 목록 — 게시글 삭제 시 함께 삭제(CASCADE + orphanRemoval) */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    /** 첨부파일 목록 — 게시글 삭제 전 FileService.delete() 로 디스크 파일도 제거 */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostFile> files = new ArrayList<>();

    /** 좋아요 목록 — (post_id, user_id) UNIQUE 제약으로 중복 방지 */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostLike> likes = new ArrayList<>();

    /** 수정 직전 updatedAt 자동 갱신 */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 본문 HTML 에서 첫 번째 &lt;img&gt; 의 src 를 추출해 반환 (없으면 null)
     * 게시판 목록 썸네일 표시에 사용
     */
    @Transient
    public String getThumbnailUrl() {
        if (content == null || content.isEmpty()) return null;
        Matcher m = Pattern.compile(
                "<img[^>]+src=[\"']([^\"']+)[\"']",
                Pattern.CASE_INSENSITIVE
        ).matcher(content);
        return m.find() ? m.group(1) : null;
    }

    /**
     * 본문 HTML 에 &lt;video&gt; 태그가 포함되어 있으면 true 반환
     * 썸네일 이미지가 없을 때 동영상 아이콘을 표시하기 위해 사용
     */
    @Transient
    public boolean isVideoPresent() {
        return content != null && content.contains("<video");
    }

    /** 게시글 상세 조회 시 호출 — 조회수 1 증가 */
    public void increaseViewCount() {
        this.viewCount++;
    }
}