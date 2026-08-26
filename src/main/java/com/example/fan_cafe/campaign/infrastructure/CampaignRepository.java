package com.example.fan_cafe.campaign.infrastructure;

import com.example.fan_cafe.campaign.domain.Campaign;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Campaign c where c.id = :campaignId")
    Optional<Campaign> findByIdForUpdate(@Param("campaignId") Long campaignId);

    @Query(value = """
            SELECT *
            FROM campaigns
            WHERE status = 'OPEN'
              AND deadline_at <= :now
            ORDER BY deadline_at, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<Campaign> findNextExpiredOpenCampaignForUpdateSkipLocked(
            @Param("now") LocalDateTime now);
}
