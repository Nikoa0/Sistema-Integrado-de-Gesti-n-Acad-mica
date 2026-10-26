package com.ipt.siga.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sigaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("SIGA IPT API")
                .description("Academic management system of the Instituto Profesional de Tecnología")
                .version("1.0.0"));
    }
}