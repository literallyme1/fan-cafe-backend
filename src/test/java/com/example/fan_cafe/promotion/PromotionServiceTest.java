package com.example.fan_cafe.promotion;


import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.promotion.application.PromotionService;
import com.example.fan_cafe.promotion.domain.Promotion;
import com.example.fan_cafe.promotion.exception.PromotionErrorCode;
import com.example.fan_cafe.promotion.infrastructure.PromotionRepository;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private S3Uploader s3Uploader;

    @InjectMocks
    PromotionService promotionService;

    Promotion promotion;

    @BeforeEach
    void setUp(){

        PromotionRequest request = PromotionRequest.builder()
                .title("할인행사_티셔츠")
                .description("싸요 많이 사세요")
                .startAt(LocalDate.parse("2025-07-10"))
                .endAt(LocalDate.parse("2025-07-11"))
                .build();
        promotion = Promotion.of(request, "https://your-bucket.s3.amazonaws.com/promotion/abc123.jpg");

    }

    @Test
    void update_shouldReplaceImage_whenNewImageIsUploaded() {
        Long id = 1L;
        MultipartFile image = mock(MultipartFile.class);
        PromotionRequest request = PromotionRequest.builder()
                .title("할인행사_응원봉")
                .description("싸요 많이 사세요")
                .startAt(LocalDate.parse("2025-07-10"))
                .endAt(LocalDate.parse("2025-07-11"))
                .build();

        when(image.isEmpty()).thenReturn(false);
        when(promotionRepository.findById(id)).thenReturn(Optional.of(promotion));
        when(s3Uploader.upload(image, "promotion")).thenReturn("new-url.jpg");

        //when
        var response = promotionService.update(id, request, image);

        //then
        verify(s3Uploader).upload(image, "promotion");
        assertEquals("new-url.jpg", response.getImageUrl());
    }

    @Test
    void shouldDeleteMerchandise_whenValidIdIsGiven(){
        Long id = 1L;
        when(promotionRepository.findById(id)).thenReturn(Optional.of(promotion));

        //when
        promotionService.delete(id);

        //then
        assertNotNull(promotion.getDeletedAt());
    }

    @Test
    void findByIdOrThrow_shouldThrowException_whenIdIsInvalid() {
        Long invalidId = 888L;
        when(promotionRepository.findById(invalidId)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class, () -> {
            promotionService.delete(invalidId);
        });

        // then
        assertEquals(PromotionErrorCode.PROMOTION_NOT_FOUND, exception.getErrorCode());

    }


}
