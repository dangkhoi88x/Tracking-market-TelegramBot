package com.example.trackingbot.service.alert;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NaturalLanguageAlertParserTest {

    private final NaturalLanguageAlertParser parser = new NaturalLanguageAlertParser();

    @Test
    void parse_shouldUnderstandVietnameseAboveAlert() {
        var parsed = parser.parse("nhac toi khi btc vuot 70000");

        assertThat(parsed).isPresent();
        assertThat(parsed.get().symbol()).isEqualTo("BTC");
        assertThat(parsed.get().operator()).isEqualTo(">");
        assertThat(parsed.get().targetPrice()).isEqualByComparingTo(new BigDecimal("70000"));
    }

    @Test
    void parse_shouldUnderstandVietnameseWithAccentsAndShortPrice() {
        var parsed = parser.parse("báo tôi lúc ethereum dưới 3.5k");

        assertThat(parsed).isPresent();
        assertThat(parsed.get().symbol()).isEqualTo("ETH");
        assertThat(parsed.get().operator()).isEqualTo("<");
        assertThat(parsed.get().targetPrice()).isEqualByComparingTo(new BigDecimal("3500"));
    }

    @Test
    void parse_shouldIgnoreRegularCommands() {
        assertThat(parser.parse("/crypto BTC")).isEmpty();
    }

    @Test
    void parse_shouldUnderstandThousandSeparators() {
        var parsed = parser.parse("nhac toi khi bitcoin vuot 70.000");

        assertThat(parsed).isPresent();
        assertThat(parsed.get().symbol()).isEqualTo("BTC");
        assertThat(parsed.get().operator()).isEqualTo(">");
        assertThat(parsed.get().targetPrice()).isEqualByComparingTo(new BigDecimal("70000"));
    }
}
