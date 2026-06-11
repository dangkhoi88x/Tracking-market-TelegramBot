package com.example.trackingbot.service.crypto;

import com.example.trackingbot.dto.response.CryptoPrice;
import com.example.trackingbot.dto.response.UsdtVndRate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ValueConversionService {

    private final CryptoPriceService cryptoPriceService;
    private final UsdtRateService usdtRateService;

    public ValueConversionService(CryptoPriceService cryptoPriceService, UsdtRateService usdtRateService) {
        this.cryptoPriceService = cryptoPriceService;
        this.usdtRateService = usdtRateService;
    }

    public String getValueMessage(String rawArguments) {
        ValueCommand command = parseValueCommand(rawArguments);
        CryptoPrice cryptoPrice = cryptoPriceService.getCurrentPrice(command.symbol());
        UsdtVndRate usdtRate = usdtRateService.getUsdtVndRate();

        BigDecimal valueUsdt = cryptoPrice.priceUsd().multiply(command.amount());
        BigDecimal valueVnd = valueUsdt.multiply(usdtRate.priceVnd());

        return """
                Value %s %s

                %s: %s USDT
                VND: %s VND

                Gia %s: %s USDT
                USDT/VND: %s
                """.formatted(
                formatAmount(command.amount()),
                cryptoPrice.symbol(),
                cryptoPrice.symbol(),
                formatMoney(valueUsdt),
                usdtRateService.formatVnd(valueVnd),
                cryptoPrice.symbol(),
                formatMoney(cryptoPrice.priceUsd()),
                usdtRateService.formatVnd(usdtRate.priceVnd())
        );
    }

    public String getHelpMessage() {
        return """
                Cach dung:
                /val 1 BTC
                /val 0.5 ETH

                Bot se tinh value theo USDT va VND.
                """;
    }

    private ValueCommand parseValueCommand(String rawArguments) {
        if (rawArguments == null || rawArguments.isBlank()) {
            throw new IllegalArgumentException("Missing value arguments");
        }

        String[] parts = rawArguments.trim().split("\\s+");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid value format");
        }

        BigDecimal amount = parsePositiveNumber(parts[0]);
        String symbol = cryptoPriceService.normalizeSymbol(parts[1]);

        if (cryptoPriceService.findCoinId(symbol).isEmpty()) {
            throw new IllegalArgumentException("Unsupported crypto symbol: " + symbol);
        }

        return new ValueCommand(amount, symbol);
    }

    private BigDecimal parsePositiveNumber(String rawNumber) {
        try {
            BigDecimal value = new BigDecimal(rawNumber.replace(",", ""));
            if (value.signum() <= 0) {
                throw new IllegalArgumentException("Number must be positive");
            }

            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid number", exception);
        }
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "N/A";
        }

        if (value.compareTo(BigDecimal.ONE) < 0) {
            return value.setScale(6, RoundingMode.HALF_UP).toPlainString();
        }

        return "%,.2f".formatted(value);
    }

    private String formatAmount(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private record ValueCommand(
            BigDecimal amount,
            String symbol
    ) {
    }
}
