package com.example.trackingbot.service.command;

public interface CommandHandler {

    CommandHandleResult handle(Long chatId, String commandText);

    default boolean isCommand(String commandText, String command) {
        return commandText.equalsIgnoreCase(command)
                || commandText.toLowerCase().startsWith(command.toLowerCase() + " ");
    }

    default String extractCommandArgument(String commandText) {
        String[] parts = commandText.split("\\s+", 2);
        if (parts.length < 2) {
            return "";
        }

        return parts[1];
    }
}
