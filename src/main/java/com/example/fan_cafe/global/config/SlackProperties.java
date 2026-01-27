package com.example.fan_cafe.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "slack.webhook") //application-properties 가져옴.
public class SlackProperties {

    //slack 활성화 여부
    private boolean enabled;

    //slack url
    private String url;
}
