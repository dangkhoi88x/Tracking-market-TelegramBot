package com.example.trackingbot.repository;

import com.example.trackingbot.entity.WatchlistItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItemEntity, Long> {

    boolean existsByUserChatIdAndSymbol(Long chatId, String symbol);

    long deleteByUserChatIdAndSymbol(Long chatId, String symbol);

    List<WatchlistItemEntity> findByUserChatIdOrderBySymbolAsc(Long chatId);

    List<WatchlistItemEntity> findAllByOrderByUserChatIdAscSymbolAsc();
}
