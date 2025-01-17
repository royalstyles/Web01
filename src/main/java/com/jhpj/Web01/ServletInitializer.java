package com.jhpj.Web01;

import org.springframework.boot.builder.SpringApplicationBuilder;

public class ServletInitializer {

    protected SpringApplicationBuilder configure(SpringApplicationBuilder application){
        return application.sources(ServletInitializer.class);
    }
}
