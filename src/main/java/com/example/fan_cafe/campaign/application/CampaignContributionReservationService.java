package com.example.fan_cafe.campaign.application;

import com.example.fan_cafe.campaign.domain.Campaign;
import com.example.fan_cafe.campaign.domain.Contribution;
import com.example.fan_cafe.campaign.exception.CampaignErrorCode;
import com.example.fan_cafe.campaign.infrastructure.CampaignRepository;
import com.example.fan_cafe.campaign.infrastructure.ContributionRepository;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.saga.application.SagaTransactionService;
import com.example.fan_cafe.order.saga.application.PaymentApprovalOutboxService;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.exception.UserErrorCode;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CampaignContributionReservationService {
    private final CampaignRepository campaignRepository;
    private final ContributionRepository contributionRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SagaTransactionService sagaTransactionService;
    private final PaymentApprovalOutboxService paymentApprovalOutboxService;
    private final Clock clock;

    @Transactional
    public CampaignContributionReservation reserve(
            User requester,
            Long campaignId,
            BigDecimal amount,
            String paymentKey
    ) {
        User user = userRepository.findByIdAndDeletedAtIsNull(requester.getId())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        Campaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new CustomException(CampaignErrorCode.CAMPAIGN_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);
        campaign.reserve(amount, now);

        Order order = orderRepository.save(Order.campaignContributionPaymentPending(user, amount));
        Contribution contribution = contributionRepository.save(
                Contribution.reserved(campaign, user, order, amount, now));
        sagaTransactionService.startCampaignPaymentPending(order.getId());
        paymentApprovalOutboxService.save(order.getId(), amount, amount, paymentKey);
        return new CampaignContributionReservation(contribution.getId(), order.getId());
    }
}
