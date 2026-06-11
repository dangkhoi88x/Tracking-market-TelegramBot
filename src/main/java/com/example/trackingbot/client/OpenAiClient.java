package com.example.trackingbot.client;

import com.example.trackingbot.config.OpenAiProperties;
import com.example.trackingbot.dto.response.AiPredictionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private static final String SYSTEM_PROMPT = """
            You are a professional crypto quant market analyst.
            Use only the provided market data. Do not invent prices, indicators, news, or hidden context.
            Do not give financial advice. Do not tell the user to buy or sell.
            Produce a balanced scenario analysis with explicit invalidation and risk context.
            Return JSON only and follow the provided schema exactly.
            """;

    private final RestClient restClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiClient(RestClient.Builder restClientBuilder, OpenAiProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrlOrDefault())
                .build();
        this.properties = properties;
    }

    public AiPredictionResponse analyze(Map<String, Object> marketData) {
        if (!properties.hasApiKey()) {
            throw new IllegalStateException("Missing OpenAI API key");
        }

        Map<String, Object> response = restClient.post()
                .uri("/v1/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .body(buildRequest(marketData))
                .retrieve()
                .body(Map.class);

        String outputText = extractOutputText(response);
        try {
            return objectMapper.readValue(outputText, AiPredictionResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot parse OpenAI AI prediction response", exception);
        }
    }

    private Map<String, Object> buildRequest(Map<String, Object> marketData) {
        return Map.of(
                "model", properties.modelOrDefault(),
                "input", List.of(
                        Map.of(
                                "role", "system",
                                "content", List.of(Map.of(
                                        "type", "input_text",
                                        "text", SYSTEM_PROMPT
                                ))
                        ),
                        Map.of(
                                "role", "user",
                                "content", List.of(Map.of(
                                        "type", "input_text",
                                        "text", buildUserPrompt(marketData)
                                ))
                        )
                ),
                "max_output_tokens", properties.maxOutputTokensOrDefault(),
                "text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", "quant_market_analysis",
                                "strict", true,
                                "schema", buildSchema()
                        )
                )
        );
    }

    private String buildUserPrompt(Map<String, Object> marketData) {
        try {
            return """
                    Analyze this crypto market data as a professional quant market analyst.

                    Requirements:
                    - Bias must be Bullish, Bearish, or Neutral.
                    - Confidence must be a realistic 0-100 score based on evidence quality.
                    - Explain context from trend, momentum, volume delta, breakout, trendline, and order flow.
                    - Give both bullish and bearish scenarios.
                    - Include invalidation, key levels, and risk management.
                    - Create chartAnnotations using only levels from MARKET_DATA.
                    - chartAnnotations are used by a renderer, so all numeric levels must be valid numbers.
                    - Keep each field concise enough for Telegram but still professional.

                    MARKET_DATA:
                    %s
                    """.formatted(objectMapper.writeValueAsString(marketData));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize market data for OpenAI", exception);
        }
    }

    private Map<String, Object> buildSchema() {
        Map<String, Object> stringField = Map.of("type", "string");
        Map<String, Object> stringListField = Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "minItems", 3,
                "maxItems", 6
        );

        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of(
                        "bias",
                        "confidence",
                        "riskLevel",
                        "marketRegime",
                        "executiveSummary",
                        "evidence",
                        "bullishScenario",
                        "bearishScenario",
                        "invalidation",
                        "keyLevels",
                        "riskManagement",
                        "watchlistTriggers",
                        "chartAnnotations"
                ),
                "properties", Map.ofEntries(
                        Map.entry("bias", Map.of(
                                "type", "string",
                                "enum", List.of("Bullish", "Bearish", "Neutral")
                        )),
                        Map.entry("confidence", Map.of(
                                "type", "integer",
                                "minimum", 0,
                                "maximum", 100
                        )),
                        Map.entry("riskLevel", Map.of(
                                "type", "string",
                                "enum", List.of("Low", "Medium", "High")
                        )),
                        Map.entry("marketRegime", stringField),
                        Map.entry("executiveSummary", stringField),
                        Map.entry("evidence", stringListField),
                        Map.entry("bullishScenario", stringField),
                        Map.entry("bearishScenario", stringField),
                        Map.entry("invalidation", stringField),
                        Map.entry("keyLevels", stringField),
                        Map.entry("riskManagement", stringField),
                        Map.entry("watchlistTriggers", stringListField),
                        Map.entry("chartAnnotations", buildChartAnnotationsSchema())
                )
        );
    }

    private Map<String, Object> buildChartAnnotationsSchema() {
        Map<String, Object> numberField = Map.of("type", "number");
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of(
                        "rangeLow",
                        "rangeHigh",
                        "currentPrice",
                        "ema20",
                        "ema50",
                        "bullishTrigger",
                        "bearishTrigger",
                        "invalidationBullish",
                        "resistanceZoneLow",
                        "resistanceZoneHigh",
                        "supportZoneLow",
                        "supportZoneHigh",
                        "bias",
                        "confidence"
                ),
                "properties", Map.ofEntries(
                        Map.entry("rangeLow", numberField),
                        Map.entry("rangeHigh", numberField),
                        Map.entry("currentPrice", numberField),
                        Map.entry("ema20", numberField),
                        Map.entry("ema50", numberField),
                        Map.entry("bullishTrigger", numberField),
                        Map.entry("bearishTrigger", numberField),
                        Map.entry("invalidationBullish", numberField),
                        Map.entry("resistanceZoneLow", numberField),
                        Map.entry("resistanceZoneHigh", numberField),
                        Map.entry("supportZoneLow", numberField),
                        Map.entry("supportZoneHigh", numberField),
                        Map.entry("bias", Map.of(
                                "type", "string",
                                "enum", List.of("Bullish", "Bearish", "Neutral")
                        )),
                        Map.entry("confidence", Map.of(
                                "type", "integer",
                                "minimum", 0,
                                "maximum", 100
                        ))
                )
        );
    }

    private String extractOutputText(Map<String, Object> response) {
        JsonNode root = objectMapper.valueToTree(response);
        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) {
                    continue;
                }

                for (JsonNode contentItem : content) {
                    if ("output_text".equals(contentItem.path("type").asText())) {
                        String text = contentItem.path("text").asText();
                        if (!text.isBlank()) {
                            return text;
                        }
                    }
                }
            }
        }

        throw new IllegalStateException("OpenAI response did not contain output_text");
    }
}
