package com.stormhacks2026.choco_cookies;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.config.import=", "spring.datasource.url=jdbc:h2:mem:context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate", "catalog.import=false", "tournaments.import=false"})
class ChocoCookiesApplicationTests {

	@Test
	void contextLoads() {
	}

}
