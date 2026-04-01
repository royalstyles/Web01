package com.jhpj.Web01;

import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * 외부 WAS(톰캣 등) 배포를 위한 서블릿 초기화 클래스
 * WAR 파일로 빌드 후 외부 서블릿 컨테이너에 배포할 경우 이 클래스가 애플리케이션 컨텍스트를 등록
 * 실질적인 부트스트랩 로직은 Web01Application#configure 와 동일
 */
public class ServletInitializer {

    /** 외부 서블릿 컨테이너가 호출 — 애플리케이션 소스를 Spring Boot 빌더에 등록 */
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application){
        return application.sources(ServletInitializer.class);
    }
}
