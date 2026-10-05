package com.yunus.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {


        final String securitySchemeName = "bearerAuth";


        return new OpenAPI()
                .info(new Info()
                        .title("E-Ticaret Mikroservis API")
                        .version("1.0.0")
                        .description("Spring Boot, Spring Security ve JWT ile geliştirilmiş e-ticaret backend API'si")
                        .contact(new Contact()
                                .name("Yunus Emre")
                                .email("yunusbtn43@gmail.com")
                                .url("https://github.com/YunusBtn"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))


                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))

                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Login'den dönen JWT token'ı yapıştırın ('Bearer ' ön eki OLMADAN :))")));
    }


}
