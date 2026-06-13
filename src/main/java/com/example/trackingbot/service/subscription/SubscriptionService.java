package com.example.trackingbot.service.subscription;

import com.example.trackingbot.config.TelegramBotProperties;
import com.example.trackingbot.entity.AiUsageQuotaEntity;
import com.example.trackingbot.entity.TelegramUser;
import com.example.trackingbot.model.SubscriptionPlan;
import com.example.trackingbot.repository.AiUsageQuotaRepository;
import com.example.trackingbot.repository.TelegramUserRepository;
import com.example.trackingbot.service.telegram.TelegramUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final String AI_FEATURE = "AI_ANALYSIS";
    private static final ZoneId QUOTA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final TelegramBotProperties telegramBotProperties;
    private final TelegramUserService telegramUserService;
    private final TelegramUserRepository telegramUserRepository;
    private final AiUsageQuotaRepository aiUsageQuotaRepository;

    @Transactional
    public QuotaDecision consumeAiQuota(Long chatId) {
        TelegramUser user = telegramUserService.getOrCreateUser(chatId);
        SubscriptionPlan plan = normalizePlan(user.getPlan());
        LocalDate today = LocalDate.now(QUOTA_ZONE);

        if (plan.unlimited()) {
            return QuotaDecision.allowed(plan, 0, Integer.MAX_VALUE);
        }

        AiUsageQuotaEntity quota = getOrCreateTodayQuota(user, today);

        if (quota.getUsedCount() >= plan.dailyAiQuota()) {
            return QuotaDecision.rejected(plan, quota.getUsedCount(), plan.dailyAiQuota());
        }

        quota.increase();
        return QuotaDecision.allowed(plan, quota.getUsedCount(), plan.dailyAiQuota());
    }

    @Transactional
    public String getUsageMessage(Long chatId) {
        TelegramUser user = telegramUserService.getOrCreateUser(chatId);
        SubscriptionPlan plan = normalizePlan(user.getPlan());
        LocalDate today = LocalDate.now(QUOTA_ZONE);
        int used = getTodayAiUsage(user, today);

        if (plan.unlimited()) {
            return """
                    Usage cua ban

                    Plan: ADMIN
                    AI quota hom nay: Unlimited
                    Da dung: %d luot
                    """.formatted(used);
        }

        return """
                Usage cua ban

                Plan: %s
                AI quota hom nay: %d/%d
                Con lai: %d luot
                Reset theo ngay Viet Nam.
                """.formatted(
                plan.name(),
                used,
                plan.dailyAiQuota(),
                Math.max(0, plan.dailyAiQuota() - used)
        );
    }

    @Transactional
    public String setPlan(Long adminChatId, String arguments) {
        String accessDeniedMessage = getAccessDeniedMessage(adminChatId);
        if (accessDeniedMessage != null) {
            return accessDeniedMessage;
        }

        String[] parts = arguments == null ? new String[0] : arguments.trim().split("\\s+");
        if (parts.length != 2) {
            return getSetPlanHelpMessage();
        }

        Long targetChatId;
        try {
            targetChatId = Long.parseLong(parts[0]);
        } catch (NumberFormatException exception) {
            return getSetPlanHelpMessage();
        }

        SubscriptionPlan plan;
        try {
            plan = SubscriptionPlan.valueOf(parts[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return getSetPlanHelpMessage();
        }

        TelegramUser user = telegramUserService.getOrCreateUser(targetChatId);
        user.setPlan(plan);
        telegramUserRepository.save(user);

        return """
                Da cap nhat subscription plan.

                Chat ID: %d
                Plan: %s
                AI quota: %s
                """.formatted(
                targetChatId,
                plan.name(),
                plan.unlimited() ? "Unlimited" : plan.dailyAiQuota() + " luot/ngay"
        );
    }

    public String getSetPlanHelpMessage() {
        return """
                Cach dung:
                /admin_set_plan CHAT_ID PLAN

                Vi du:
                /admin_set_plan 123456789 PRO

                Plan hop le:
                FREE - 5 AI/ngay
                PRO - 50 AI/ngay
                ADMIN - unlimited
                """;
    }

    public String buildQuotaExceededMessage(QuotaDecision decision) {
        return """
                Ban da het AI quota hom nay.

                Plan: %s
                Da dung: %d/%d luot

                Dung /my_usage de xem usage.
                """.formatted(
                decision.plan().name(),
                decision.used(),
                decision.limit()
        );
    }

    private int getTodayAiUsage(TelegramUser user, LocalDate today) {
        return aiUsageQuotaRepository.findByUserAndFeatureAndUsageDate(user, AI_FEATURE, today)
                .map(AiUsageQuotaEntity::getUsedCount)
                .orElse(0);
    }

    private AiUsageQuotaEntity getOrCreateTodayQuota(TelegramUser user, LocalDate today) {
        aiUsageQuotaRepository.insertIfMissing(user.getId(), AI_FEATURE, today);
        return aiUsageQuotaRepository.findByUserAndFeatureAndUsageDateForUpdate(user, AI_FEATURE, today)
                .orElseThrow(() -> new IllegalStateException("AI quota row was not created"));
    }

    private SubscriptionPlan normalizePlan(SubscriptionPlan plan) {
        return plan == null ? SubscriptionPlan.FREE : plan;
    }

    private String getAccessDeniedMessage(Long chatId) {
        String adminChatId = telegramBotProperties.adminChatId();
        if (adminChatId == null || adminChatId.isBlank()) {
            return """
                    Chua cau hinh TELEGRAM_ADMIN_CHAT_ID.

                    De bat lenh admin, lay chat_id cua ban trong bang telegram_users roi them env:
                    TELEGRAM_ADMIN_CHAT_ID=chat_id_cua_ban
                    """;
        }

        if (!adminChatId.equals(String.valueOf(chatId))) {
            return "Lenh admin chi danh cho owner cua bot.";
        }

        return null;
    }

    public record QuotaDecision(
            boolean allowed,
            SubscriptionPlan plan,
            int used,
            int limit
    ) {

        static QuotaDecision allowed(SubscriptionPlan plan, int used, int limit) {
            return new QuotaDecision(true, plan, used, limit);
        }

        static QuotaDecision rejected(SubscriptionPlan plan, int used, int limit) {
            return new QuotaDecision(false, plan, used, limit);
        }
    }
}
