package com.example.trackingbot.repository;

import com.example.trackingbot.entity.NotificationHistoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistoryEntity, Long> {

    boolean existsByNotificationId(String notificationId);

    List<NotificationHistoryEntity> findByChatIdOrderBySentAtDesc(Long chatId, Pageable pageable);
}
