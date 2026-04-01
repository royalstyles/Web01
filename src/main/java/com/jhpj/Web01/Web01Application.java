package com.jhpj.Web01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Spring Boot 애플리케이션 진입점
 * - SpringBootServletInitializer 상속으로 WAR 배포도 지원 (내장 톰캣 + 외부 WAS 겸용)
 * - @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan 통합
 */
@SpringBootApplication
public class Web01Application extends SpringBootServletInitializer {

	/**
	 * WAR 배포 시 외부 서블릿 컨테이너가 이 메서드를 호출해 애플리케이션 초기화
	 * 내장 톰캣 실행(main) 과 동일한 소스를 등록
	 */
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application){
		return application.sources(Web01Application.class);
	}

	/** 로컬/도커 환경에서 내장 톰캣으로 직접 실행할 때의 진입점 */
	public static void main(String[] args) {
		SpringApplication.run(Web01Application.class, args);
	}

}
