package com.example.fan_cafe.campaign.application;

import com.example.fan_cafe.campaign.domain.ContributionStatus;
import com.example.fan_cafe.campaign.infrastructure.CampaignRepository;
import com.example.fan_cafe.campaign.infrastructure.ContributionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CampaignDeadlineTransactionService {
    private final CampaignRepository campaignRepository;
    private final ContributionRepository contributionRepository;

    @Transactional
    public Optional<Long> closeNext(LocalDateTime now) {
        return campaignRepository.findNextExpiredOpenCampaignForUpdateSkipLocked(now)
                .map(campaign -> {
                    campaign.failAndStartRefunding(now);
                    if (!contributionRepository.existsByCampaignIdAndStatusIn(
                            campaign.getId(),
                            List.of(ContributionStatus.CONFIRMED, ContributionStatus.REFUNDING))) {
                        campaign.completeRefunding(now);
                    }
                    return campaign.getId();
                });
    }
}
