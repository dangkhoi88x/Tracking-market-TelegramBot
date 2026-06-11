package com.example.trackingbot.service.telegram;

import com.example.trackingbot.entity.TelegramUser;
import com.example.trackingbot.repository.TelegramUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TelegramUserService {

    private final TelegramUserRepository telegramUserRepository;

    @Transactional
    public TelegramUser getOrCreateUser(Long chatId) {
        return telegramUserRepository.findByChatId(chatId)
                .orElseGet(() -> telegramUserRepository.save(new TelegramUser(chatId)));
    }
}
