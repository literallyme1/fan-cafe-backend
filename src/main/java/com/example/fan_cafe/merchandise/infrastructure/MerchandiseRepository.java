package com.example.fan_cafe.merchandise.infrastructure;

import com.example.fan_cafe.merchandise.domain.Merchandise;
import jakarta.persistence.LockModeType;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchandiseRepository extends JpaRepository<Merchandise, Long>, MerchandiseRepositoryCustom {

    // 삭제되지 않은 단일 상품 조회.
    @NonNull
    Optional<Merchandise> findByIdAndDeletedAtIsNull(Long id);

    // 재고 차감 전 동시성 문제를 막기 위해 쓰기 락을 걸고 조회.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Merchandise m where m.id = :id and m.deletedAt is null")
    Optional<Merchandise> findByIdAndDeletedAtIsNullForUpdate(Long id);

    // 주문 요청에 포함된 상품들을 한 번에 조회할 때 사용.
    @NonNull
    List<Merchandise> findAllByIdInAndDeletedAtIsNull(@NonNull List<Long> ids);

}
