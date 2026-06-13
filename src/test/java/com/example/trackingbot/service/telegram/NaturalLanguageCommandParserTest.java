package com.example.trackingbot.service.telegram;

import org.junit.jupiter.api.Test;

import static com.example.trackingbot.service.telegram.NaturalLanguageCommandParser.NaturalLanguageCommandType.BUY;
import static com.example.trackingbot.service.telegram.NaturalLanguageCommandParser.NaturalLanguageCommandType.CHART;
import static com.example.trackingbot.service.telegram.NaturalLanguageCommandParser.NaturalLanguageCommandType.PRICE;
import static com.example.trackingbot.service.telegram.NaturalLanguageCommandParser.NaturalLanguageCommandType.SELL;
import static org.assertj.core.api.Assertions.assertThat;

class NaturalLanguageCommandParserTest {

    private final NaturalLanguageCommandParser parser = new NaturalLanguageCommandParser();

    @Test
    void parse_shouldUnderstandPriceQuestion() {
        var command = parser.parse("gia btc");

        assertThat(command).isPresent();
        assertThat(command.get().type()).isEqualTo(PRICE);
        assertThat(command.get().symbol()).isEqualTo("BTC");
    }

    @Test
    void parse_shouldUnderstandChartRequest() {
        var command = parser.parse("ve chart eth 7 ngay");

        assertThat(command).isPresent();
        assertThat(command.get().type()).isEqualTo(CHART);
        assertThat(command.get().symbol()).isEqualTo("ETH");
        assertThat(command.get().arguments()).isEqualTo("7d");
    }

    @Test
    void parse_shouldUnderstandBuyWithoutAmount() {
        var command = parser.parse("mua btc gia 65000");

        assertThat(command).isPresent();
        assertThat(command.get().type()).isEqualTo(BUY);
        assertThat(command.get().arguments()).isEqualTo("BTC 65000");
    }

    @Test
    void parse_shouldUnderstandBuyWithAmount() {
        var command = parser.parse("mua btc 0.1 gia 65k");

        assertThat(command).isPresent();
        assertThat(command.get().type()).isEqualTo(BUY);
        assertThat(command.get().arguments()).isEqualTo("BTC 0.1 65000");
    }

    @Test
    void parse_shouldUnderstandSell() {
        var command = parser.parse("ban sol gia 200");

        assertThat(command).isPresent();
        assertThat(command.get().type()).isEqualTo(SELL);
        assertThat(command.get().arguments()).isEqualTo("SOL 200");
    }

    @Test
    void parse_shouldNotMistakeBanAsSellWhenAskingPrice() {
        var command = parser.parse("ban xem gia btc");

        assertThat(command).isPresent();
        assertThat(command.get().type()).isEqualTo(PRICE);
        assertThat(command.get().symbol()).isEqualTo("BTC");
    }

    @Test
    void parse_shouldIgnoreSlashCommands() {
        assertThat(parser.parse("/crypto BTC")).isEmpty();
    }
}
