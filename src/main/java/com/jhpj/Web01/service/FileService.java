package com.jhpj.Web01.service;

import com.jhpj.Web01.entity.Post;
import com.jhpj.Web01.entity.PostFile;
import com.jhpj.Web01.entity.User;
import com.jhpj.Web01.repository.PostFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final PostFileRepository postFileRepository;

    @Value("${app.upload.path}")
    private String uploadPath;

    @Value("${app.upload.url-prefix}")
    private String urlPrefix;

    // 허용 이미지 MIME
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    // 허용 동영상 MIME
    private static final List<String> ALLOWED_VIDEO_TYPES = List.of(
            "video/mp4", "video/webm", "video/ogg", "video/quicktime"
    );

    // 최대 이미지 크기: 20MB
    private static final long MAX_IMAGE_SIZE = 20L * 1024 * 1024;

    // 최대 동영상 크기: 500MB
    private static final long MAX_VIDEO_SIZE = 500L * 1024 * 1024;

    /**
     * 파일 업로드 — DB 저장 후 PostFile 반환
     * post는 null로 저장 (게시글 저장 시 attachToPost 호출로 연결)
     */
    @Transactional
    public PostFile upload(MultipartFile file, User uploader) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("파일 형식을 확인할 수 없습니다.");
        }

        PostFile.FileType fileType = resolveFileType(contentType);
        validateFileSize(file.getSize(), fileType);

        // 저장 디렉토리: /uploads/images/ 또는 /uploads/videos/
        String subDir = (fileType == PostFile.FileType.IMAGE) ? "images" : "videos";
        Path dir = Paths.get(uploadPath, subDir);
        Files.createDirectories(dir);

        // UUID 기반 저장 파일명 (확장자 유지)
        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);

        Path filePath = dir.resolve(storedName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = urlPrefix + "/" + subDir + "/" + storedName;

        PostFile postFile = PostFile.builder()
                .post(null)                         // 임시 상태
                .uploader(uploader)
                .originalName(originalName != null ? originalName : storedName)
                .storedName(storedName)
                .filePath(filePath.toAbsolutePath().toString())
                .fileUrl(fileUrl)
                .contentType(contentType)
                .fileSize(file.getSize())
                .fileType(fileType)
                .build();

        return postFileRepository.save(postFile);
    }

    /**
     * 파일 삭제 — 디스크 + DB 동시 삭제
     */
    @Transactional
    public void delete(String storedName) {
        postFileRepository.findByStoredName(storedName).ifPresent(pf -> {
            try {
                Files.deleteIfExists(Paths.get(pf.getFilePath()));
            } catch (IOException e) {
                log.warn("파일 삭제 실패: {}", pf.getFilePath(), e);
            }
            postFileRepository.delete(pf);
        });
    }

    /**
     * 게시글에 파일 연결 (임시 → 정식)
     * storedNames: Quill.js 본문에 포함된 파일 URL에서 추출한 저장 파일명 목록
     */
    @Transactional
    public void attachFilesToPost(Post post, List<String> storedNames) {
        if (storedNames == null || storedNames.isEmpty()) return;

        storedNames.forEach(name ->
                postFileRepository.findByStoredName(name).ifPresent(pf -> {
                    pf.attachToPost(post);
                    postFileRepository.save(pf);
                })
        );
    }

    /**
     * 고아 파일 정리 스케줄러 — 매 1시간마다 POST_ID가 없는 임시 파일 삭제
     */
    @Scheduled(fixedDelay = 60 * 60 * 1000)
    @Transactional
    public void cleanOrphanFiles() {
        LocalDateTime before = LocalDateTime.now().minusHours(1);
        List<PostFile> orphans = postFileRepository.findOrphanFiles(before);

        orphans.forEach(pf -> {
            try {
                Files.deleteIfExists(Paths.get(pf.getFilePath()));
                log.info("고아 파일 삭제: {}", pf.getStoredName());
            } catch (IOException e) {
                log.warn("고아 파일 디스크 삭제 실패: {}", pf.getFilePath(), e);
            }
            postFileRepository.delete(pf);
        });

        if (!orphans.isEmpty()) {
            log.info("고아 파일 정리 완료: {}개", orphans.size());
        }
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────

    private PostFile.FileType resolveFileType(String contentType) {
        if (ALLOWED_IMAGE_TYPES.contains(contentType)) return PostFile.FileType.IMAGE;
        if (ALLOWED_VIDEO_TYPES.contains(contentType)) return PostFile.FileType.VIDEO;
        throw new IllegalArgumentException("허용되지 않는 파일 형식입니다: " + contentType);
    }

    private void validateFileSize(long size, PostFile.FileType fileType) {
        long max = (fileType == PostFile.FileType.IMAGE) ? MAX_IMAGE_SIZE : MAX_VIDEO_SIZE;
        if (size > max) {
            String limit = (fileType == PostFile.FileType.IMAGE) ? "20MB" : "500MB";
            throw new IllegalArgumentException(
                    fileType.name().toLowerCase() + " 파일 크기는 " + limit + " 이하여야 합니다.");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}