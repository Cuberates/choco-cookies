package com.stormhacks2026.choco_cookies.catalog.featured;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:featuredcommand;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate", "catalog.import=false", "tournaments.import=false", "catalog.featured.refresh=false"})
class FeaturedCommandContextTests {
    @Test void operatorCommandsCanStartWithoutServletSecurityBeans() {}
}
