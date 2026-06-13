package com.example.trackingbot.repository;

import com.example.trackingbot.entity.AiUsageQuotaEntity;
import com.example.trackingbot.entity.TelegramUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface AiUsageQuotaRepository extends JpaRepository<AiUsageQuotaEntity, Long> {

    Optional<AiUsageQuotaEntity> findByUserAndFeatureAndUsageDate(
            TelegramUser user,
            String feature,
            LocalDate usageDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select quota
            from AiUsageQuotaEntity quota
            where quota.user = :user
              and quota.feature = :feature
              and quota.usageDate = :usageDate
            """)
    Optional<AiUsageQuotaEntity> findByUserAndFeatureAndUsageDateForUpdate(
            TelegramUser user,
            String feature,
            LocalDate usageDate
    );

    @Modifying
    @Query(value = """
            insert into ai_usage_quotas (user_id, feature, usage_date, used_count, created_at, updated_at)
            values (:userId, :feature, :usageDate, 0, current_timestamp, current_timestamp)
            on conflict (user_id, feature, usage_date) do nothing
            """, nativeQuery = true)
    void insertIfMissing(Long userId, String feature, LocalDate usageDate);
}
