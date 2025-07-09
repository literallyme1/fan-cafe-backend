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
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

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
        var response = merchandiseService.update(id, request);
        //then
        assertAll(
                () -> assertEquals(Status.SOLD_OUT, response.getStatus()),
                () -> assertEquals(request.getName(), response.getName())
        );
    }

    @Test
    void decreaseStock_shouldThrowOutOfStockError_whenQuantityExceedsStock(){
        int quantity = 105;

        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            merchandiseService.decreaseStock(id, quantity);
        });

        //then
        assertEquals(MerchandiseErrorCode.OUT_OF_STOCK, exception.getErrorCode());
    }

    @Test
    void decreaseStock_shouldMarkSoldOut_whenStockBecomesZero(){
        int quantity = 100;

        //when
        var response = merchandiseService.decreaseStock(id, quantity);

        //then
        assertEquals(Status.SOLD_OUT, response.getStatus());
    }

    @Test
    void shouldDeleteMerchandise_whenValidIdIsGiven(){
        merchandiseService.delete(id);
        assertNotNull(merchandise.getDeletedAt());
    }
}
