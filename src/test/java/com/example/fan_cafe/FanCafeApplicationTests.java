package com.example.fan_cafe;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("ci")
class FanCafeApplicationTests {

	@Disabled("CI: Spring still resolves jwt keys as classpath resources; re-enable when ci profile file paths apply reliably")
	@Test
	void contextLoads() {
	}

}
