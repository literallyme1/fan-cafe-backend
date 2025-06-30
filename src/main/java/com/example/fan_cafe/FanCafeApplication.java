package com.example.fan_cafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class FanCafeApplication {

	public static void main(String[] args) {
		SpringApplication.run(FanCafeApplication.class, args);
	}

}
