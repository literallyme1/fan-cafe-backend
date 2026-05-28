package com.example.fan_cafe.outbox.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Outbox 수동 재시도 관리자 화면.
 * GET /admin/outbox-manual-retry      → 목록 페이지 (API 기반)
 * GET /admin/outbox-manual-retry/{id} → 상세 페이지 (API 기반)
 */
@Controller
@RequestMapping("/admin/outbox-manual-retry")
public class OutboxManualRetryAdminController {

    @GetMapping
    public String list() {
        return "admin/outbox/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id) {
        return "admin/outbox/detail";
    }
}
