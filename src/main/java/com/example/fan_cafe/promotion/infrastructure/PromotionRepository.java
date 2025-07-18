package com.example.fan_cafe.promotion.infrastructure;

import com.example.fan_cafe.promotion.domain.Promotion;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long>, PromotionRepositoryCustom {

}
