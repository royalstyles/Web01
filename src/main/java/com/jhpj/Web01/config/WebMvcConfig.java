package com.jhpj.Web01.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 추가 설정 클래스
 * - 업로드 파일 정적 리소스 핸들러 등록 (/uploads/** → NAS 실제 디렉토리)
 * - 뒤로가기 캐시 방지 인터셉터 등록 (게시판 목록/홈 경로)
 * - @EnableScheduling 으로 FileService 의 고아 파일 정리 스케줄러 활성화
 */
@Configuration
@EnableScheduling   // FileService 고아 파일 정리 스케줄러 활성화
public class WebMvcConfig implements WebMvcConfigurer {

    /** 파일이 실제 저장된 서버 절대 경로 — application.properties 의 app.upload.path */
    @Value("${app.upload.path}")
    private String uploadPath;

    /** URL 접두사 — application.properties 의 app.upload.url-prefix (기본: /uploads) */
    @Value("${app.upload.url-prefix}")
    private String urlPrefix;

    /**
     * /uploads/** 요청을 NAS 실제 디렉토리로 매핑
     * 브라우저에서 /uploads/images/uuid.jpg 접근 가능
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }

    // ── 뒤로가기 캐시 방지 인터셉터 추가 ──────────────────
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request,
                                             HttpServletResponse response,
                                             Object handler) {
                        // 게시판 목록/상세 페이지는 캐시 없이 항상 서버에서 새로 로드
                        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                        response.setHeader("Pragma", "no-cache");
                        response.setDateHeader("Expires", 0);
                        return true;
                    }
                })
                // 정적 리소스 제외, 게시판 관련 경로만 적용
                .addPathPatterns("/board/**", "/home", "/")
                .excludePathPatterns("/uploads/**", "/css/**", "/js/**", "/favicon.svg");
    }
}