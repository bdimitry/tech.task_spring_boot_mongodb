package com.example.task.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Notes API",
                version = "v1",
                description = "REST API for notes service"
        )
)
public class OpenApiConfig {
}
