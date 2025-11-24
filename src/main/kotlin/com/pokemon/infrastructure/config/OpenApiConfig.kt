package com.pokemon.infrastructure.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
                .info(
                        Info().title("Pokemon Backend API")
                                .version("1.0.0")
                                .description(
                                        "REST API for authentication. All Pokemon operations are available via GraphQL at /graphql and /graphql-playground"
                                )
                                .contact(Contact().name("Pokemon Backend Team"))
                                .license(
                                        License()
                                                .name("Apache 2.0")
                                                .url(
                                                        "https://www.apache.org/licenses/LICENSE-2.0.html"
                                                )
                                )
                )
    }
}
