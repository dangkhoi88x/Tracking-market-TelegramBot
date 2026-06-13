package com.example.trackingbot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisRateLimitStore implements RateLimitStore {

    private static final DefaultRedisScript<String> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            for index, key in ipairs(KEYS) do
                local offset = (index - 1) * 4
                local now = tonumber(ARGV[offset + 1])
                local window = tonumber(ARGV[offset + 2])
                local maxRequests = tonumber(ARGV[offset + 3])

                redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
                local currentCount = tonumber(redis.call('ZCARD', key))

                if currentCount >= maxRequests then
                    local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
                    local retryAfterSeconds = math.ceil(window / 1000)

                    if oldest[2] then
                        retryAfterSeconds = math.ceil((tonumber(oldest[2]) + window - now) / 1000)
                    end

                    if retryAfterSeconds < 1 then
                        retryAfterSeconds = 1
                    end

                    return 'REJECT|' .. index .. '|' .. retryAfterSeconds
                end
            end

            for index, key in ipairs(KEYS) do
                local offset = (index - 1) * 4
                local now = tonumber(ARGV[offset + 1])
                local window = tonumber(ARGV[offset + 2])
                local member = ARGV[offset + 4]

                redis.call('ZADD', key, now, member)
                redis.call('PEXPIRE', key, window)
            end

            return 'ALLOW|0|0'
            """, String.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public RateLimitStoreResult checkAndIncrement(List<RateLimitStoreRequest> requests) {
        if (requests.isEmpty()) {
            return RateLimitStoreResult.allow();
        }

        List<String> keys = requests.stream()
                .map(RateLimitStoreRequest::key)
                .toList();
        List<String> args = new ArrayList<>(requests.size() * 4);

        for (RateLimitStoreRequest request : requests) {
            args.add(String.valueOf(request.now().toEpochMilli()));
            args.add(String.valueOf(request.window().toMillis()));
            args.add(String.valueOf(request.maxRequests()));
            args.add(request.now().toEpochMilli() + ":" + UUID.randomUUID());
        }

        String result = redisTemplate.execute(RATE_LIMIT_SCRIPT, keys, args.toArray());
        return parseResult(result);
    }

    private RateLimitStoreResult parseResult(String result) {
        if (result == null || result.isBlank()) {
            return RateLimitStoreResult.allow();
        }

        String[] parts = result.split("\\|");
        if (parts.length != 3 || "ALLOW".equals(parts[0])) {
            return RateLimitStoreResult.allow();
        }

        int rejectedIndex = Integer.parseInt(parts[1]) - 1;
        long retryAfterSeconds = Long.parseLong(parts[2]);
        return RateLimitStoreResult.reject(rejectedIndex, retryAfterSeconds);
    }
}
