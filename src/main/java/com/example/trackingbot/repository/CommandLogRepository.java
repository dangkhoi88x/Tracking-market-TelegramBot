package com.example.trackingbot.repository;

import com.example.trackingbot.entity.CommandLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface CommandLogRepository extends JpaRepository<CommandLogEntity, Long> {

    @Query("""
            select log.command as command,
                   count(log.id) as totalCount,
                   sum(case when log.success = true then 1 else 0 end) as successCount,
                   sum(case when log.success = false then 1 else 0 end) as errorCount,
                   avg(log.durationMs) as averageDurationMs
            from CommandLogEntity log
            where log.createdAt >= :since
            group by log.command
            order by count(log.id) desc
            """)
    List<TopCommandProjection> findTopCommandsSince(Instant since, Pageable pageable);

    @Query("""
            select log
            from CommandLogEntity log
            where log.success = false
            order by log.createdAt desc
            """)
    List<CommandLogEntity> findRecentErrors(Pageable pageable);

    @Query("""
            select log.chatId as chatId,
                   count(log.id) as totalCount,
                   max(log.createdAt) as lastCommandAt
            from CommandLogEntity log
            where log.createdAt >= :since
            group by log.chatId
            order by count(log.id) desc
            """)
    List<TopUserProjection> findTopUsersSince(Instant since, Pageable pageable);

    @Query("select count(distinct log.chatId) from CommandLogEntity log")
    long countDistinctUsers();

    @Query("select count(distinct log.chatId) from CommandLogEntity log where log.createdAt >= :since")
    long countDistinctUsersSince(Instant since);

    interface TopCommandProjection {
        String getCommand();

        long getTotalCount();

        long getSuccessCount();

        long getErrorCount();

        Double getAverageDurationMs();
    }

    interface TopUserProjection {
        Long getChatId();

        long getTotalCount();

        Instant getLastCommandAt();
    }
}
