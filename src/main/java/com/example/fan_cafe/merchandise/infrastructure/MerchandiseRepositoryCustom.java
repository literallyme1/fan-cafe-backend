package com.example.fan_cafe.merchandise.infrastructure;

import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.domain.Status;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface MerchandiseRepositoryCustom {

    Slice<MerchandiseResponse> findTopByCategory(Status status, Category category, Pageable pageable);
}
