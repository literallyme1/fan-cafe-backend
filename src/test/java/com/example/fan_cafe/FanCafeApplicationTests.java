package com.example.fan_cafe;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Tag("integration")
@Disabled("CI: @SpringBootTest still boots full context (JwtProvider classpath keys); re-enable when ci JWT paths work")
@SpringBootTest
@ActiveProfiles("ci")
class FanCafeApplicationTests {

	@Test
	void contextLoads() {
	}

}
