package com.stockpilot.wms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** API docs at /swagger-ui.html. Use POST /api/login, then click "Authorize" and paste the token. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI wmsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("StockPilot WMS API")
                        .version("v1")
                        .description("Log in with POST /api/login to get a token, then send it as "
                                + "'Authorization: Bearer <token>'."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
