package com.example.trackingbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
 // Cong cu chung de goi HTTP API ben ngoai
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
