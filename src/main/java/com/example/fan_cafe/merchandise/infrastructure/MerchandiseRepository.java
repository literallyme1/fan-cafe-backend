package com.example.fan_cafe.merchandise.infrastructure;

import com.example.fan_cafe.merchandise.domain.Merchandise;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchandiseRepository extends JpaRepository<Merchandise, Long>, MerchandiseRepositoryCustom {

    @NonNull
    Optional<Merchandise> findById(Long id);

}
