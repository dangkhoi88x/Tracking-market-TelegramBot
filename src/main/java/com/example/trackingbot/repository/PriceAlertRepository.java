package com.example.trackingbot.repository;

import com.example.trackingbot.entity.PriceAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlertEntity, String> {

    List<PriceAlertEntity> findByActiveTrueOrderByCreatedAtAsc();

    List<PriceAlertEntity> findByUserChatIdAndActiveTrueOrderByCreatedAtDesc(Long chatId);

    @Modifying
    @Query("""
            delete from PriceAlertEntity alert
            where alert.active = false
              and alert.updatedAt < :cutoff
            """)
    int deleteInactiveBefore(Instant cutoff);
}
