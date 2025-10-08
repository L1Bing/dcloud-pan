package com.bing.dcloudpan.config;

import com.bing.dcloudpan.interceptor.LoginInterceptor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
public class InterceptorConfig implements WebMvcConfigurer {
    @Resource
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                //添加拦截的路径
                .addPathPatterns("/api/account/*/**","/api/file/*/**","/api/share/*/**")

                //排除不拦截
                .excludePathPatterns("/api/account/*/register","/api/account/*/login","/api/account/*/upload_avatar",
                        "/api/share/*/check_share_code","/api/share/*/visit","/api/share/*/detail_no_code","/api/share/*/detail_with_code");
    }
}
