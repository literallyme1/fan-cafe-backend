package com.example.fan_cafe.outbox.controller;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.outbox.application.DlqService;
import com.example.fan_cafe.outbox.domain.DlqEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * DLQ 이벤트 관리자 화면.
 * GET /admin/dlq      → 전체 목록 (Thymeleaf)
 * GET /admin/dlq/{id} → 상세 조회 (Thymeleaf)
 * POST /admin/dlq/{id}/retry → 수동 재처리 후 목록으로 redirect
 */
@Controller
@RequestMapping("/admin/dlq")
@RequiredArgsConstructor
@Slf4j
public class DlqAdminController {

    private final DlqService dlqService;

    /** DLQ 이벤트 목록 페이지 */
    @GetMapping
    public String list(Model model) {
        List<DlqEvent> events = dlqService.findAllOrderByNewest();
        model.addAttribute("events", events);
        return "admin/dlq/list";
    }

    /** eventId 기준으로 가장 최근 DLQ 레코드를 상세 조회 */
    @GetMapping("/{eventId}")
    public String detail(@PathVariable String eventId, Model model) {
        DlqEvent event = dlqService.getLatestByEventId(eventId);
        model.addAttribute("event", event);
        // payload 전체가 길 수 있어 200자까지만 UI에 노출한다
        String preview = event.getPayload() != null && event.getPayload().length() > 200
                ? event.getPayload().substring(0, 200) + " ..."
                : event.getPayload();
        model.addAttribute("payloadPreview", preview);
        return "admin/dlq/detail";
    }

    /**
     * RETRY_EXCEEDED 건만 메인 큐로 재발행.
     * NON_RETRYABLE이면 {@link com.example.fan_cafe.outbox.exception.DlqErrorCode#DLQ_NOT_RETRYABLE} 예외.
     */
    @PostMapping("/{eventId}/retry")
    public String retry(
            @PathVariable String eventId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            dlqService.retryToMainQueue(eventId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "eventId=" + eventId + " 재처리 요청이 완료되었습니다.");
            log.info("[DLQ ADMIN] manual retry requested eventId={}", eventId);
        } catch (CustomException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getErrorMessage());
            log.warn("[DLQ ADMIN] retry rejected eventId={}, reason={}", eventId, e.getErrorMessage());
        }
        return "redirect:/admin/dlq";
    }
}
