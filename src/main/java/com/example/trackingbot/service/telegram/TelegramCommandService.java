package com.example.trackingbot.service.telegram;

import com.example.trackingbot.service.audit.CommandLogService;
import com.example.trackingbot.service.command.CommandHandleResult;
import com.example.trackingbot.service.command.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

//Nhận text từ controller
//Kiểm tra /help
//Kiểm tra rate limit
//Chuyển command sang đúng handler
//Nếu không phải command thì thử hiểu ngôn ngữ tự nhiên
//Ghi log command
public class TelegramCommandService {

    private final TelegramMessageService telegramMessageService; //gửi tin nhắn về Telegram.
    private final TelegramKeyboardService telegramKeyboardService; //tạo inline keyboard button
    private final TelegramCallbackService telegramCallbackService; //xử lý khi user bấm button
    private final TelegramHelpService telegramHelpService; //lấy nội dung /help, /start
    private final NaturalLanguageCommandService naturalLanguageCommandService; //hiểu câu tự nhiên
    private final UserCommandRateLimiter userCommandRateLimiter; //user spam command.
    private final CommandLogService commandLogService; //lưu log command vào DB
    private final List<CommandHandler> commandHandlers; //danh sách các handler xử lý command
// xu ly text /crypto BTC
    public void handleTextMessage(Long chatId, String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        String commandText = text.trim();
        String command = extractCommandName(commandText); //lấy command đầu tiên
        long startedAt = System.nanoTime(); //tính command chạy mất bao lâu
        //ghi log cuối hàm.
        boolean success = true;
        String errorMessage = null;

        try { //xử lý command
            //gui help meny ney /help
            if (isHelpCommand(commandText))  {

                telegramMessageService.sendTextMessage(
                        chatId,
                        telegramHelpService.getMainHelpMessage(),
                        telegramKeyboardService.buildMainMenuKeyboard()
                );
                return;
            }
            // check limit
            var rateLimitResult = userCommandRateLimiter.checkAllowed(chatId, commandText);
            if (!rateLimitResult.allowed()) {
                success = false;
                errorMessage = "RATE_LIMIT: %s max %d/%s retry after %ds".formatted(
                        rateLimitResult.ruleName(),
                        rateLimitResult.maxRequests(),
                        rateLimitResult.windowLabel(),
                        rateLimitResult.retryAfterSeconds()
                );
                telegramMessageService.sendTextMessage(chatId, buildRateLimitMessage(rateLimitResult));
                return;
            }
            // step route command
            CommandHandleResult commandResult = handleSlashCommand(chatId, commandText);

            if (commandResult.handled()) {
                success = commandResult.success();
                errorMessage = commandResult.errorMessage();
                return;
            }
            //if not  slash command , try natural text (gia btc,nhac toi khi sol vuot 200)
            if (naturalLanguageCommandService.handle(chatId, commandText)) {
                return;
            }
            //Nếu không hiểu gì thì trả danh sach command goi y
            telegramMessageService.sendTextMessage(chatId, telegramHelpService.getUnknownCommandMessage());
        } catch (Exception exception) {
            success = false;
            errorMessage = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            throw exception;
        } finally {
            commandLogService.record(
                    chatId,
                    command,
                    success,
                    errorMessage,
                    durationMillisSince(startedAt)
            );
        }
    }

    public void handleCallbackQuery(String callbackQueryId, Long chatId, String callbackData) {
        telegramCallbackService.handleCallbackQuery(callbackQueryId, chatId, callbackData);
    }

    private boolean isHelpCommand(String commandText) {
        return "/start".equalsIgnoreCase(commandText) || "/help".equalsIgnoreCase(commandText);
    }
// route :Nó hỏi lần lượt từng handler: command này của bạn không? // handler nào nhận xử lý trả handled = true
    private CommandHandleResult handleSlashCommand(Long chatId, String commandText) {
        for (CommandHandler handler : commandHandlers) {
            CommandHandleResult result = handler.handle(chatId, commandText);
            if (result.handled()) {
                return result;
            }
        }
//Nếu không handler nào nhận
        return CommandHandleResult.notHandled();
    }

    private String extractCommandName(String commandText) {
        return commandText.trim().split("\\s+", 2)[0].toLowerCase();
    }
//tính thời gian xử lý
    private long durationMillisSince(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private String buildRateLimitMessage(UserCommandRateLimiter.RateLimitResult result) {
        return """
                Ban dang gui lenh qua nhanh.

                Gioi han: %s toi da %d lan/%s.
                Thu lai sau khoang %d giay.
                """.formatted(
                result.ruleName(),
                result.maxRequests(),
                result.windowLabel(),
                result.retryAfterSeconds()
        );
    }
}
