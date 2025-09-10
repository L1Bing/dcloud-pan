package com.bing.dcloudpan.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j配置 ，默认是下面
 *
 * knife4j 访问地址：http://localhost:8080/doc.html
 * Swagger2.0访问地址：http://localhost:8080/swagger-ui.html
 * Swagger3.0访问地址：http://localhost:8080/swagger-ui/index.html
 */
@Slf4j
@Configuration
public class Knife4jConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI网盘系统 API")
                        .version("1.0")
                        .description("AI网盘系统")
                        .termsOfService("https://bing.net")
                        .license(new License().name("Apache 2.0").url("https://bing.net"))
                        // 添加作者信息
                        .contact(new Contact()
                                .name("libing") // 替换为作者的名字
                                .email("15040855446@163.com") // 替换为作者的电子邮件
                                .url("https://bing.net") // 替换为作者的网站或个人资料链接
                        )
                ) ;
    }
}
