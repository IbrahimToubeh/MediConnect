package com.MediConnect.ai.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
//test g
/**
 * Dedicated HTTP client for talking to the OpenAI API.
 * <p>
 * Using a named bean keeps our service code clean and makes it easier to adjust
 * timeouts or add logging in the future without touching business logic.
 */
@Configuration
public class AiConfiguration {

    @Bean(name = "openAiRestTemplate")
    public RestTemplate openAiRestTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) java.time.Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) java.time.Duration.ofSeconds(45).toMillis());

        return builder
                .requestFactory(() -> factory)
                .build();
    }
}

