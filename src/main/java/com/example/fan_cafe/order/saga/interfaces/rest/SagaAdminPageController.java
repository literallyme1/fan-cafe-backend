package com.example.fan_cafe.order.saga.interfaces.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/saga-reconciliation")
public class SagaAdminPageController {

    @GetMapping
    public String list() {
        return "admin/outbox/sagas";
    }
}
