package com.example.fan_cafe.campaign.interfaces.rest;

import com.example.fan_cafe.campaign.application.CampaignContributionService;
import com.example.fan_cafe.campaign.application.CampaignRefundService;
import com.example.fan_cafe.campaign.application.CampaignService;
import com.example.fan_cafe.campaign.interfaces.dto.CampaignCreateRequest;
import com.example.fan_cafe.campaign.interfaces.dto.CampaignResponse;
import com.example.fan_cafe.campaign.interfaces.dto.ContributionCreateRequest;
import com.example.fan_cafe.campaign.interfaces.dto.ContributionResponse;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/campaigns")
@RequiredArgsConstructor
public class CampaignController {
    private final CampaignService campaignService;
    private final CampaignContributionService contributionService;
    private final CampaignRefundService refundService;

    @PostMapping
    public ApiResponse<CampaignResponse> create(@RequestBody @Valid CampaignCreateRequest request) {
        return ApiResponse.success(ApiResponseStatus.CREATED, campaignService.create(request));
    }

    @GetMapping
    public ApiResponse<List<CampaignResponse>> getAll() {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, campaignService.getAll());
    }

    @GetMapping("/{campaignId}")
    public ApiResponse<CampaignResponse> get(@PathVariable Long campaignId) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, campaignService.get(campaignId));
    }

    @PostMapping("/{campaignId}/contributions")
    public ApiResponse<ContributionResponse> contribute(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long campaignId,
            @RequestBody @Valid ContributionCreateRequest request
    ) {
        return ApiResponse.success(
                ApiResponseStatus.CREATED,
                contributionService.contribute(user, campaignId, request));
    }

    @PostMapping("/{campaignId}/contributions/{contributionId}/refund")
    public ApiResponse<ContributionResponse> refund(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long campaignId,
            @PathVariable Long contributionId
    ) {
        return ApiResponse.success(
                ApiResponseStatus.SUCCESS,
                refundService.requestUserRefund(user, campaignId, contributionId));
    }
}
