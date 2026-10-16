package com.flowwork.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) 接口文档配置
 * 文档地址：/swagger-ui.html，接口描述：/v3/api-docs
 * 全局声明 Bearer JWT 认证方案，便于在文档中直接调试需要登录的接口
 */
@Configuration
public class OpenApiConfig {

    /**
     * 构建 OpenAPI 文档：标题、版本、描述，并注册全局 Bearer JWT 安全方案
     */
    @Bean
    public OpenAPI flowworkOpenApi() {
        return new OpenAPI()
                .info(new Info().title("FlowWork API").version("1.0.0")
                        .description("AI 驱动团队项目协作平台接口文档"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
