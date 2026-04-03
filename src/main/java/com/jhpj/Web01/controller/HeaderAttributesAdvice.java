package com.jhpj.Web01.controller;

import com.jhpj.Web01.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 @Controller 핸들러 실행 전 헤더 프래그먼트에 필요한 공통 model 속성을 자동 주입
 * 기존에 각 컨트롤러마다 중복되어 있던 addHeaderAttributes() 를 하나로 통합
 *
 * 주입 속성:
 *   - isLoggedIn    : 로그인 여부
 *   - username      : 사용자명 (로그인 시)
 *   - isAdmin       : 관리자 여부 (ROLE_ADMIN)
 *   - profileImage  : 프로필 이미지 URL (로그인 시)
 */
@ControllerAdvice
@RequiredArgsConstructor
public class HeaderAttributesAdvice {

    private final UserRepository userRepository;

    @ModelAttribute
    public void addHeaderAttributes(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            // 로그인 상태: 사용자 정보 및 권한 주입
            boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            model.addAttribute("isLoggedIn",    true);
            model.addAttribute("username",       userDetails.getUsername());
            model.addAttribute("isAdmin",        isAdmin);
            // 프로필 이미지는 DB 조회가 필요하므로 Optional 처리
            userRepository.findByUsername(userDetails.getUsername())
                    .ifPresent(u -> model.addAttribute("profileImage", u.getProfileImage()));
        } else {
            // 비로그인 상태: 기본값 설정
            model.addAttribute("isLoggedIn", false);
        }
    }
}
