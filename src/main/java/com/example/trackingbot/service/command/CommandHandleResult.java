package com.example.trackingbot.service.command;

public record CommandHandleResult(
        boolean handled,
        boolean success,
        String errorMessage
) {

    public static CommandHandleResult notHandled() {
        return new CommandHandleResult(false, true, null);
    }

    public static CommandHandleResult handledSuccessfully() {
        return new CommandHandleResult(true, true, null);
    }

    public static CommandHandleResult failed(String errorMessage) {
        return new CommandHandleResult(true, false, errorMessage);
    }
}
