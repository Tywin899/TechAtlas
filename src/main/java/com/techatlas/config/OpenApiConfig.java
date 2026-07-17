package com.techatlas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TechAtlas API")
                        .version("1.0.0")
                        .description("Backend search engine for backend engineering knowledge, providing unified search across multiple content providers.")
                        .contact(new Contact()
                                .name("TechAtlas Team")
                                .email("support@techatlas.com")));
    }
}
