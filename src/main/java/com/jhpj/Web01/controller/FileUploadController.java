package com.jhpj.Web01.controller;

import com.jhpj.Web01.entity.PostFile;
import com.jhpj.Web01.entity.User;
import com.jhpj.Web01.repository.UserRepository;
import com.jhpj.Web01.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileService fileService;
    private final UserRepository userRepository;

    /**
     * Quill.js 이미지 업로드
     * 요청: POST /api/upload/image  (multipart: file)
     * 응답: { "url": "/uploads/images/uuid.jpg" }
     */
    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        return handleUpload(file, userDetails, "image");
    }

    /**
     * 동영상 업로드
     * 요청: POST /api/upload/video  (multipart: file)
     * 응답: { "url": "/uploads/videos/uuid.mp4", "storedName": "uuid.mp4" }
     */
    @PostMapping("/video")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        return handleUpload(file, userDetails, "video");
    }

    /**
     * 임시 파일 삭제 (게시글 작성 취소 시 에디터에서 호출)
     * 요청: DELETE /api/upload/{storedName}
     */
    @DeleteMapping("/{storedName}")
    public ResponseEntity<?> deleteFile(
            @PathVariable String storedName,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            fileService.delete(storedName);
            return ResponseEntity.ok(Map.of("message", "삭제 완료"));
        } catch (Exception e) {
            log.warn("파일 삭제 실패: {}", storedName, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── 내부 공통 처리 ───────────────────────────────────────

    private ResponseEntity<?> handleUpload(
            MultipartFile file, UserDetails userDetails, String type) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "파일이 비어있습니다."));
        }

        try {
            User uploader = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

            PostFile postFile = fileService.upload(file, uploader);

            // Quill.js는 업로드 응답에서 "url" 키를 읽어 에디터에 삽입
            return ResponseEntity.ok(Map.of(
                    "url", postFile.getFileUrl(),
                    "storedName", postFile.getStoredName()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("{} 업로드 실패", type, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "업로드 중 오류가 발생했습니다."));
        }
    }
}