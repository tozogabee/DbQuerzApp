package examp.org.com.dbquerzapp.config;

import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiTitleConfig {

    @Bean
    public GroupedOpenApi executeQueryApi() {
        return GroupedOpenApi.builder()
                .group("execute-query")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info()
                        .title("Execute Query API")
                        .version("1.0.0")
                        .description("Endpoints to run saved queries and inspect results.")
                ))
                .pathsToMatch("/execute-query/**")
                .build();
    }
}

