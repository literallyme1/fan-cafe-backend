package com.example.fan_cafe.campaign.application;

import com.example.fan_cafe.campaign.domain.Campaign;
import com.example.fan_cafe.campaign.exception.CampaignErrorCode;
import com.example.fan_cafe.campaign.infrastructure.CampaignRepository;
import com.example.fan_cafe.campaign.interfaces.dto.CampaignCreateRequest;
import com.example.fan_cafe.campaign.interfaces.dto.CampaignResponse;
import com.example.fan_cafe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignService {
    private final CampaignRepository campaignRepository;

    @Transactional
    public CampaignResponse create(CampaignCreateRequest request) {
        Campaign campaign = Campaign.open(
                request.title(), request.content(), request.targetAmount(),
                request.startsAt(), request.deadlineAt());
        return CampaignResponse.from(campaignRepository.save(campaign));
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> getAll() {
        return campaignRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(CampaignResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CampaignResponse get(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .map(CampaignResponse::from)
                .orElseThrow(() -> new CustomException(CampaignErrorCode.CAMPAIGN_NOT_FOUND));
    }
}
