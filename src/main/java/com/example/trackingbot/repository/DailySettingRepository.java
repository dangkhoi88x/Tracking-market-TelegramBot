package com.example.trackingbot.repository;

import com.example.trackingbot.entity.DailySettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DailySettingRepository extends JpaRepository<DailySettingEntity, Long> {

    Optional<DailySettingEntity> findByUserChatId(Long chatId);

    List<DailySettingEntity> findByEnabledTrueOrderByUserChatIdAsc();
}
