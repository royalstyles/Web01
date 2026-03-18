package com.jhpj.Web01.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "POST_FILES")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PostFile {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_files_seq")
    @SequenceGenerator(name = "post_files_seq", sequenceName = "POST_FILES_SEQ", allocationSize = 1)
    private Long id;

    // 임시 업로드 시 null, 게시글 저장 후 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(nullable = false, length = 500)
    private String originalName;    // 원본 파일명

    @Column(nullable = false, unique = true, length = 500)
    private String storedName;      // UUID 기반 저장 파일명

    @Column(nullable = false, length = 1000)
    private String filePath;        // 서버 절대 경로

    @Column(nullable = false, length = 1000)
    private String fileUrl;         // 브라우저 접근 URL (/uploads/xxx.jpg)

    @Column(nullable = false, length = 100)
    private String contentType;     // MIME 타입 (image/jpeg, video/mp4 등)

    @Column(nullable = false)
    private long fileSize;          // 바이트 단위

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FileType fileType;      // IMAGE / VIDEO

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum FileType {
        IMAGE, VIDEO
    }

    /** post_id 연결 (임시 → 정식) */
    public void attachToPost(Post post) {
        this.post = post;
    }
}