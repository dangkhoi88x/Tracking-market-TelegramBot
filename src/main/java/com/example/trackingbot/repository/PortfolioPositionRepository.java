package com.example.trackingbot.repository;

import com.example.trackingbot.entity.PortfolioPositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioPositionRepository extends JpaRepository<PortfolioPositionEntity, String> {

    List<PortfolioPositionEntity> findByUserChatIdOrderByCreatedAtDesc(Long chatId);
}
