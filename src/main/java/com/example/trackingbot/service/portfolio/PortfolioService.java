package com.example.trackingbot.service.portfolio;

import com.example.trackingbot.entity.PortfolioPositionEntity;
import com.example.trackingbot.model.PortfolioPosition;
import com.example.trackingbot.dto.response.CryptoPrice;
import com.example.trackingbot.repository.PortfolioPositionRepository;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.telegram.TelegramUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final String SIDE_BUY = "BUY";
    private static final String SIDE_SELL = "SELL";

    private final CryptoPriceService cryptoPriceService;
    private final TelegramUserService telegramUserService;
    private final PortfolioPositionRepository portfolioPositionRepository;

    public String addBuy(Long chatId, String rawArguments) {
        return addPosition(chatId, SIDE_BUY, rawArguments);
    }

    public String addSell(Long chatId, String rawArguments) {
        return addPosition(chatId, SIDE_SELL, rawArguments);
    }

    @Transactional(readOnly = true)
    public String getPortfolioMessage(Long chatId) {
        List<PortfolioPosition> positions = getPositions(chatId);
        if (positions.isEmpty()) {
            return """
                    Portfolio cua ban dang trong.

                    Them lenh dau tien:
                    /buy BTC 0.1 65000
                    /sell BTC 61600
                    """;
        }

        StringBuilder message = new StringBuilder("Portfolio cua ban:\n\n");
        for (PortfolioPosition position : positions) {
            try {
                CryptoPrice currentPrice = cryptoPriceService.getCurrentPrice(position.symbol());
                message.append(formatPosition(position, currentPrice));
            } catch (Exception exception) {
                message.append(position.symbol()).append(": tam thoi khong lay duoc gia hien tai\n\n");
            }
        }

        return message.toString();
    }

    public String getHelpMessage() {
        return """
                Cach dung:
                /buy BTC 0.1 65000
                /buy BTC 65000
                /sell BTC 61600
                /myportfolio

                Ghi chu:
                Co so luong coin thi bot tinh P/L USD.
                Khong co so luong thi bot tinh % so voi gia hien tai.
                """;
    }

    @Transactional
    private String addPosition(Long chatId, String side, String rawArguments) {
        ParsedPosition parsed = parsePosition(rawArguments);
        String symbol = cryptoPriceService.normalizeSymbol(parsed.symbol());

        if (cryptoPriceService.findCoinId(symbol).isEmpty()) {
            return """
                    Minh chua ho tro ma crypto nay.

                    Ma crypto dang ho tro:
                    %s
                    """.formatted(cryptoPriceService.getSupportedSymbolsText());
        }

        String positionId = UUID.randomUUID().toString();
        portfolioPositionRepository.save(new PortfolioPositionEntity(
                positionId,
                telegramUserService.getOrCreateUser(chatId),
                side,
                symbol,
                parsed.amount(),
                parsed.entryPrice()
        ));

        return """
                Da luu %s position.

                %s %s
                Entry: %s USD
                ID: %s
                """.formatted(
                side,
                side,
                symbol,
                formatMoney(parsed.entryPrice()),
                positionId
        );
    }

    private ParsedPosition parsePosition(String rawArguments) {
        if (rawArguments == null || rawArguments.isBlank()) {
            throw new IllegalArgumentException("Missing portfolio arguments");
        }

        String[] parts = rawArguments.trim().split("\\s+");
        if (parts.length != 2 && parts.length != 3) {
            throw new IllegalArgumentException("Invalid portfolio format");
        }

        String symbol = parts[0];
        BigDecimal amount = null;
        BigDecimal entryPrice;

        if (parts.length == 2) {
            entryPrice = parsePositiveNumber(parts[1]);
        } else {
            amount = parsePositiveNumber(parts[1]);
            entryPrice = parsePositiveNumber(parts[2]);
        }

        return new ParsedPosition(symbol, amount, entryPrice);
    }

    private List<PortfolioPosition> getPositions(Long chatId) {
        return portfolioPositionRepository.findByUserChatIdOrderByCreatedAtDesc(chatId)
                .stream()
                .map(this::toPortfolioPosition)
                .toList();
    }

    private PortfolioPosition toPortfolioPosition(PortfolioPositionEntity entity) {
        return new PortfolioPosition(
                entity.getId(),
                entity.getUser().getChatId(),
                entity.getSide(),
                entity.getSymbol(),
                entity.getAmount(),
                entity.getEntryPrice()
        );
    }

    private String formatPosition(PortfolioPosition position, CryptoPrice currentPrice) {
        BigDecimal current = currentPrice.priceUsd();
        BigDecimal entry = position.entryPrice();

        if (SIDE_SELL.equals(position.side())) {
            return formatSellPosition(position, current, entry);
        }

        return formatBuyPosition(position, current, entry);
    }

    private String formatBuyPosition(PortfolioPosition position, BigDecimal current, BigDecimal entry) {
        BigDecimal percent = current.subtract(entry)
                .divide(entry, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        StringBuilder message = new StringBuilder();
        message.append("%s BUY\n".formatted(position.symbol()));
        if (position.amount() != null) {
            BigDecimal profitLoss = current.subtract(entry).multiply(position.amount());
            message.append("Amount: %s coin\n".formatted(formatAmount(position.amount())));
            message.append("Gia mua: %s USD\n".formatted(formatMoney(entry)));
            message.append("Gia hien tai: %s USD\n".formatted(formatMoney(current)));
            message.append("P/L: %s USD (%s%%)\n\n".formatted(formatSignedMoney(profitLoss), formatSignedPercent(percent)));
            return message.toString();
        }

        message.append("Entry: %s USD\n".formatted(formatMoney(entry)));
        message.append("Now: %s USD\n".formatted(formatMoney(current)));
        message.append("Move: %s%%\n\n".formatted(formatSignedPercent(percent)));
        return message.toString();
    }

    private String formatSellPosition(PortfolioPosition position, BigDecimal current, BigDecimal entry) {
        BigDecimal buyBackPercent = entry.divide(current, 6, RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100));

        StringBuilder message = new StringBuilder();
        message.append("%s SELL\n".formatted(position.symbol()));
        message.append("Sell: %s USD\n".formatted(formatMoney(entry)));
        message.append("Now: %s USD\n".formatted(formatMoney(current)));

        if (position.amount() != null) {
            BigDecimal profitLoss = entry.subtract(current).multiply(position.amount());
            message.append("Amount: %s coin\n".formatted(formatAmount(position.amount())));
            message.append("P/L: %s USD\n".formatted(formatSignedMoney(profitLoss)));
        }

        message.append("Buy now to %s%% %s %s\n\n".formatted(
                formatSignedPercent(buyBackPercent),
                position.symbol(),
                buyBackPercent.signum() > 0 ? "OK" : "WAIT"
        ));
        return message.toString();
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

        return "%,.2f".formatted(value);
    }

    private String formatSignedMoney(BigDecimal value) {
        String formatted = formatMoney(value);
        if (value != null && value.signum() > 0) {
            return "+" + formatted;
        }

        return formatted;
    }

    private String formatAmount(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String formatSignedPercent(BigDecimal value) {
        if (value == null) {
            return "N/A";
        }

        BigDecimal rounded = value.setScale(2, RoundingMode.HALF_UP);
        if (rounded.signum() > 0) {
            return "+" + rounded;
        }

        return rounded.toString();
    }

    private record ParsedPosition(
            String symbol,
            BigDecimal amount,
            BigDecimal entryPrice
    ) {
    }
}
