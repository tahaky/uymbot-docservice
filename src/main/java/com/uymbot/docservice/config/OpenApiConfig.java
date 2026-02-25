package com.uymbot.docservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DocService – Vector DB Document API")
                        .description("CRUD operations for documents stored in a ChromaDB vector database.")
                        .version("1.0.0"));
    }
}
