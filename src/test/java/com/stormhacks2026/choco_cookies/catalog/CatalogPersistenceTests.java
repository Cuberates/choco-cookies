package com.stormhacks2026.choco_cookies.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.config.import=", "catalog.import=false"})
@EnabledIfEnvironmentVariable(named = "CATALOG_TEST_DATABASE_URL", matches = ".+")
class CatalogPersistenceTests {
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("CATALOG_TEST_DATABASE_URL"));
        properties.add("spring.datasource.username", () -> System.getenv("CATALOG_TEST_DATABASE_USERNAME"));
        properties.add("spring.datasource.password", () -> System.getenv("CATALOG_TEST_DATABASE_PASSWORD"));
    }
    @Autowired BallImportWriter writer;
    @Autowired BowlingBallRepository repository;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entityManager;
    @Test @org.springframework.transaction.annotation.Transactional
    void migrationsAndRepeatedImportsPreserveIdentityAndReplaceWeights() {
        String url = BowwwlParser.ROOT + "/test/persistence-" + java.util.UUID.randomUUID();
        var first = new BallData(url, url, null, "Fixture Ball", null, "Solid Reactive", "Symmetric", Set.of(12, 15));
        assertTrue(writer.upsert(first));
        Long id = repository.findBySourceKey(url).orElseThrow().getId();
        var changed = new BallData(url, url, null, "Updated Ball", "Fixture Brand", null, null, Set.of(14, 16));
        assertFalse(writer.upsert(changed));
        assertFalse(writer.upsert(changed));
        entityManager.clear();
        var ball = repository.findBySourceKey(url).orElseThrow();
        assertEquals(Set.of(14, 16), ball.getWeights());
        assertEquals(id, ball.getId()); assertEquals("Updated Ball", ball.getName());
        assertNull(ball.getCoverstockType()); assertNull(ball.getCoreType());
        repository.deleteById(id);
    }
}
