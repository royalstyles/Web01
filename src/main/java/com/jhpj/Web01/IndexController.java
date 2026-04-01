package com.jhpj.Web01;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 루트 경로(/) 처리용 컨트롤러 — 현재 미사용 상태
 * 실제 루트 요청은 HomeController#root 에서 /home 으로 리다이렉트 처리
 * 초기 Hello World 확인용 코드가 주석으로 남아 있음
 */
@RestController
public class IndexController {
//    @GetMapping("/")
//    public String index() {
//        return "Hello World!";  // 초기 서버 동작 확인용 임시 코드
//    }
}
