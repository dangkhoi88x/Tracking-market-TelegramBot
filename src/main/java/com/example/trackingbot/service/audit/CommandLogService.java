package com.example.trackingbot.service.audit;

import com.example.trackingbot.entity.CommandLogEntity;
import com.example.trackingbot.repository.CommandLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommandLogService {

    private static final Logger log = LoggerFactory.getLogger(CommandLogService.class);
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1_000;

    private final CommandLogRepository commandLogRepository;

    public void record(Long chatId, String command, boolean success, String errorMessage, long durationMs) {
        try {
            commandLogRepository.save(new CommandLogEntity(
                    chatId,
                    normalizeCommand(command),
                    success,
                    normalizeErrorMessage(errorMessage),
                    durationMs
            ));
        } catch (Exception exception) {
            log.warn("Failed to write command log chatId={} command={}", chatId, command, exception);
        }
    }

    private String normalizeCommand(String command) {
        if (command == null || command.isBlank()) {
            return "UNKNOWN";
        }

        return command.length() <= 100 ? command : command.substring(0, 100);
    }

    private String normalizeErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }

        return errorMessage.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? errorMessage
                : errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
