package com.example.authswitch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Forces Swagger to use a relative server URL ("/"). Behind an HTTPS proxy
 * (Railway/Fly), the app sees plain HTTP and would otherwise advertise an
 * http:// URL, which the browser blocks from an https page ("Failed to fetch").
 * A relative URL makes Swagger call the API on the same origin/scheme as the page.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authSwitchOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Authorization Switch")
                        .description("Real-time card authorization (ISO 8583 0100 -> 0110)")
                        .version("v0.1.0"))
                .servers(List.of(new Server().url("/").description("Same-origin")));
    }
}