package com.example.fan_cafe.merchandise.infrastructure;

import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.domain.Status;
import io.lettuce.core.dynamic.annotation.Param;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchandiseRepository extends JpaRepository<Merchandise, Long> {

    @NonNull
    Optional<Merchandise> findById(Long id);

    @Query("SELECT m FROM Merchandise m " +
           "WHERE m.deletedAt IS NULL " +
            "AND m.status = :status " +
            "AND (:category IS NULL OR m.category = :category)")
    Slice<Merchandise> searchMerchandise(@Param("status") Status status,
                                        @Param("category")Category category,
                                         Pageable pageable);
}
