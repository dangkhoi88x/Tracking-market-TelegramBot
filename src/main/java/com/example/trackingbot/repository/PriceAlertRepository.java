package com.example.trackingbot.repository;

import com.example.trackingbot.entity.PriceAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlertEntity, String> {

    List<PriceAlertEntity> findByActiveTrueOrderByCreatedAtAsc();

    List<PriceAlertEntity> findByUserChatIdAndActiveTrueOrderByCreatedAtDesc(Long chatId);
}
