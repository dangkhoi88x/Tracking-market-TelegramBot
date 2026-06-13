package com.example.trackingbot.service.subscription;

import com.example.trackingbot.config.TelegramBotProperties;
import com.example.trackingbot.entity.AiUsageQuotaEntity;
import com.example.trackingbot.entity.TelegramUser;
import com.example.trackingbot.model.SubscriptionPlan;
import com.example.trackingbot.repository.AiUsageQuotaRepository;
import com.example.trackingbot.repository.TelegramUserRepository;
import com.example.trackingbot.service.telegram.TelegramUserService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class SubscriptionServiceTest {

    @Test
    void consumeAiQuota_shouldLimitFreeUserToFiveAiCallsPerDay() {
        TestContext context = new TestContext(SubscriptionPlan.FREE);

        for (int index = 0; index < 5; index++) {
            assertThat(context.service.consumeAiQuota(123L).allowed()).isTrue();
        }

        SubscriptionService.QuotaDecision decision = context.service.consumeAiQuota(123L);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.plan()).isEqualTo(SubscriptionPlan.FREE);
        assertThat(decision.used()).isEqualTo(5);
        assertThat(decision.limit()).isEqualTo(5);
    }

    @Test
    void consumeAiQuota_shouldAllowProUserToUseFiftyAiCallsPerDay() {
        TestContext context = new TestContext(SubscriptionPlan.PRO);

        for (int index = 0; index < 50; index++) {
            assertThat(context.service.consumeAiQuota(123L).allowed()).isTrue();
        }

        SubscriptionService.QuotaDecision decision = context.service.consumeAiQuota(123L);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.used()).isEqualTo(50);
        assertThat(decision.limit()).isEqualTo(50);
    }

    @Test
    void consumeAiQuota_shouldNotLimitAdminPlan() {
        TestContext context = new TestContext(SubscriptionPlan.ADMIN);

        for (int index = 0; index < 100; index++) {
            SubscriptionService.QuotaDecision decision = context.service.consumeAiQuota(123L);

            assertThat(decision.allowed()).isTrue();
            assertThat(decision.plan()).isEqualTo(SubscriptionPlan.ADMIN);
        }
    }

    private static class TestContext {

        private final SubscriptionService service;

        TestContext(SubscriptionPlan plan) {
            TelegramUser user = new TelegramUser(123L);
            user.setPlan(plan);

            TelegramUserService telegramUserService = mock(TelegramUserService.class);
            TelegramUserRepository telegramUserRepository = mock(TelegramUserRepository.class);
            AiUsageQuotaRepository aiUsageQuotaRepository = mock(AiUsageQuotaRepository.class);
            Map<String, AiUsageQuotaEntity> quotas = new HashMap<>();

            when(telegramUserService.getOrCreateUser(123L)).thenReturn(user);
            when(telegramUserRepository.save(any(TelegramUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(aiUsageQuotaRepository.findByUserAndFeatureAndUsageDateForUpdate(
                    any(TelegramUser.class),
                    any(String.class),
                    any(LocalDate.class)
            )).thenAnswer(invocation -> Optional.ofNullable(quotas.get(key(
                    invocation.getArgument(0),
                    invocation.getArgument(1),
                    invocation.getArgument(2)
            ))));
            doAnswer(invocation -> {
                Long userId = invocation.getArgument(0);
                String feature = invocation.getArgument(1);
                LocalDate usageDate = invocation.getArgument(2);
                String key = userId + ":" + feature + ":" + usageDate;
                quotas.putIfAbsent(key, new AiUsageQuotaEntity(user, feature, usageDate));
                return null;
            }).when(aiUsageQuotaRepository).insertIfMissing(any(), any(String.class), any(LocalDate.class));
            when(aiUsageQuotaRepository.save(any(AiUsageQuotaEntity.class))).thenAnswer(invocation -> {
                AiUsageQuotaEntity quota = invocation.getArgument(0);
                quotas.put(key(quota.getUser(), quota.getFeature(), quota.getUsageDate()), quota);
                return quota;
            });

            service = new SubscriptionService(
                    new TelegramBotProperties("token", "secret", "999"),
                    telegramUserService,
                    telegramUserRepository,
                    aiUsageQuotaRepository
            );
        }

        private String key(TelegramUser user, String feature, LocalDate usageDate) {
            return user.getId() + ":" + feature + ":" + usageDate;
        }
    }
}
