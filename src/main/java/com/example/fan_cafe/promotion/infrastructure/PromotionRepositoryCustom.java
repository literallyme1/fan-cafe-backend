package com.example.fan_cafe.promotion.infrastructure;

import com.example.fan_cafe.promotion.interfaces.dto.PromotionResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface PromotionRepositoryCustom {

    Slice<PromotionResponse> findSliceBy(Pageable pageable);
}
