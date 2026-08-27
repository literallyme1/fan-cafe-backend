package com.example.fan_cafe.campaign.domain;

import com.example.fan_cafe.global.exception.CustomException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampaignTest {
    private final LocalDateTime startsAt = LocalDateTime.of(2026, 8, 26, 10, 0);
    private final LocalDateTime deadlineAt = startsAt.plusDays(7);

    @Test
    void campaignStartsOpenWithCampaignAmountsAsSourceOfTruth() {
        Campaign campaign = campaign("100000");

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.OPEN);
        assertThat(campaign.getTargetAmount()).isEqualByComparingTo("100000");
        assertThat(campaign.getFundedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(campaign.getReservedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void reservationAndConfirmationMoveAmountWithoutChangingTheTotalCommittedAmount() {
        Campaign campaign = campaign("100000");

        campaign.reserve(new BigDecimal("30000"), startsAt.plusHours(1));
        campaign.confirmReservation(new BigDecimal("30000"));

        assertThat(campaign.getReservedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(campaign.getFundedAmount()).isEqualByComparingTo("30000");
    }

    @Test
    void amountMustBeAtLeastOneThousandAndUseOneThousandWonUnits() {
        assertThatThrownBy(() -> campaign("999"))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> campaign("1500"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void fundedAndReservedAmountsCannotExceedTarget() {
        Campaign campaign = campaign("10000");
        campaign.reserve(new BigDecimal("9000"), startsAt.plusHours(1));

        assertThatThrownBy(() -> campaign.reserve(
                new BigDecimal("2000"), startsAt.plusHours(1)))
                .isInstanceOf(CustomException.class);
    }

    private Campaign campaign(String targetAmount) {
        return Campaign.open(
                "birthday support",
                "campaign content",
                new BigDecimal(targetAmount),
                startsAt,
                deadlineAt);
    }
}
