package com.example.trackingbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        // gom config tu yaml
        String apiKey,
        String baseUrl,
        String model,
        int maxOutputTokens
) {
    // ham default
    public String baseUrlOrDefault() {
        return baseUrl == null || baseUrl.isBlank()
                ? "https://api.openai.com"
                : baseUrl;
    }

    public String modelOrDefault() {
        return model == null || model.isBlank() ? "gpt-5-mini" : model;
    }

    public int maxOutputTokensOrDefault() {
        return maxOutputTokens <= 0 ? 2200 : maxOutputTokens;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
