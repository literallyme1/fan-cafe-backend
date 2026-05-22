package com.example.fan_cafe;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("ci")
class FanCafeApplicationTests {

	@Test
	void contextLoads() {
	}

}
