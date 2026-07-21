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

    @NonNull
    Optional<Merchandise> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Merchandise m where m.id = :id and m.deletedAt is null")
    Optional<Merchandise> findMerchandiseWithPessimisticLock(Long id);

    @NonNull
    List<Merchandise> findAllByIdInAndDeletedAtIsNull(@NonNull List<Long> ids);

}
