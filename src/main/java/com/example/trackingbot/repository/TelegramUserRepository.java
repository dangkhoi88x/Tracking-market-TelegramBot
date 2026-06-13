package com.example.trackingbot.repository;

import com.example.trackingbot.entity.TelegramUser;
import com.example.trackingbot.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {
    Optional<TelegramUser> findByChatId(Long chatId);

    long countByPlan(SubscriptionPlan plan);
}
