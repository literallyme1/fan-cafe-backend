package com.example.fan_cafe.global.test;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Slf4j
public class LogTestController {

//    @GetMapping("/log")
//    public String logTest() {
//        log.info("log test");
//        return "ok";
//    }
//
//    @GetMapping("/error-test")
//    public String errorTest() {
//        throw new RuntimeException("boom");
//    }


}
