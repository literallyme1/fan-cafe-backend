package com.example.fan_cafe.campaign.infrastructure;

import com.example.fan_cafe.campaign.domain.Contribution;
import com.example.fan_cafe.campaign.domain.ContributionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    Optional<Contribution> findByOrderId(Long orderId);

    Optional<Contribution> findByIdAndUserId(Long contributionId, Long userId);

    @Query("""
            select c.order.id
            from Contribution c
            where c.id = :contributionId
              and c.user.id = :userId
            """)
    Optional<Long> findOrderIdByIdAndUserId(
            @Param("contributionId") Long contributionId,
            @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Contribution c where c.id = :contributionId")
    Optional<Contribution> findByIdForUpdate(@Param("contributionId") Long contributionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Contribution c where c.id = :contributionId and c.user.id = :userId")
    Optional<Contribution> findByIdAndUserIdForUpdate(
            @Param("contributionId") Long contributionId,
            @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Contribution c where c.order.id = :orderId")
    Optional<Contribution> findByOrderIdForUpdate(@Param("orderId") Long orderId);

    List<Contribution> findAllByCampaignIdAndStatusOrderById(
            Long campaignId,
            ContributionStatus status,
            Pageable pageable);

    List<Contribution> findAllByCampaignIdOrderById(Long campaignId);

    @Query("""
            select c.order.id
            from Contribution c
            where c.campaign.id = :campaignId
              and c.status = :status
            order by c.id
            """)
    List<Long> findOrderIdsByCampaignIdAndStatus(
            @Param("campaignId") Long campaignId,
            @Param("status") ContributionStatus status);

    boolean existsByCampaignIdAndStatusIn(
            Long campaignId,
            Collection<ContributionStatus> statuses);

    @Query("""
            select distinct c.user.id
            from Contribution c
            where c.campaign.id = :campaignId
              and c.status = :status
            order by c.user.id
            """)
    List<Long> findDistinctUserIdsByCampaignIdAndStatus(
            @Param("campaignId") Long campaignId,
            @Param("status") ContributionStatus status);
}
