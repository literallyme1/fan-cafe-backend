package com.example.fan_cafe.merchandise;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.merchandise.application.MerchandiseService;
import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.domain.Status;
import com.example.fan_cafe.merchandise.exception.MerchandiseErrorCode;
import com.example.fan_cafe.merchandise.infrastructure.MerchandiseRepository;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MerchandiseServiceTest {

    @Mock
    private MerchandiseRepository merchandiseRepository;

    @Mock
    private S3Uploader s3Uploader;

    @InjectMocks
    private MerchandiseService merchandiseService;

    private Merchandise merchandise;
    private Long id;

    @BeforeEach
    void setUp() {

        MerchandiseRequest request = MerchandiseRequest.builder()
                .name("응원봉")
                .description("응원봉입니다")
                .price(1000L)
                .salePrice(900L)
                .stock(100)
                .status(Status.SALE)
                .category(Category.LIGHT_STICK)
                .build();

        id = 1L;
        merchandise = Merchandise.of(request, null);
        ReflectionTestUtils.setField(merchandise, "id", 1L);
        when(merchandiseRepository.findById(1L)).thenReturn(Optional.of(merchandise));
    }

    @Test
    void update_shouldMarkSoldOut_whenStockIsZero() {

        //given
        MerchandiseRequest request = MerchandiseRequest.builder()
                .name("티셔츠")
                .description("티셔츠입니다")
                .price(1000L)
                .salePrice(900L)
                .stock(0)
                .status(Status.SALE)
                .category(Category.LIGHT_STICK)
                .build();

        //when
        var response = merchandiseService.update(id, request, null);

        //then
        assertThat(response.getStatus()).isEqualTo(Status.SOLD_OUT);
        assertThat(response.getName()).isEqualTo(request.getName());
    }

    @Test
    void decreaseStock_shouldThrowOutOfStockError_whenQuantityExceedsStock(){
        int quantity = 105;

        //when
        assertThrows(CustomException.class, () -> {
            merchandiseService.decreaseStock(id, quantity);
        });
    }

    @Test
    void decreaseStock_shouldMarkSoldOut_whenStockBecomesZero(){
        int quantity = 100;

        //when
        var response = merchandiseService.decreaseStock(id, quantity);

        //then
        assertThat(response.getStatus()).isEqualTo(Status.SOLD_OUT);
    }

    @Test
    void decreaseStock_shouldThrowException_whenStockBecomesNegative(){
        //given
        int quantity = 9999;

        //when & then
        assertThrows(CustomException.class, () -> {
            merchandiseService.decreaseStock(id, quantity);
        });
    }

    @Test
    void shouldDeleteMerchandise_whenValidIdIsGiven(){
        merchandiseService.delete(id);
        assertThat(merchandise.getDeletedAt()).isNotNull();
    }
}
