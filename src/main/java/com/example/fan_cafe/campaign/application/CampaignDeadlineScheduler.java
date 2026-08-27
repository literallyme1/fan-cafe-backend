package com.example.fan_cafe.campaign.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "campaign.deadline.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CampaignDeadlineScheduler {
    private final CampaignDeadlineWorker worker;
    private final int batchSize;

    public CampaignDeadlineScheduler(
            CampaignDeadlineWorker worker,
            @org.springframework.beans.factory.annotation.Value("${campaign.deadline.batch-size:20}") int batchSize
    ) {
        this.worker = worker;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${campaign.deadline.fixed-delay:5000}")
    public void process() {
        worker.processDueCampaigns(batchSize);
    }
}
