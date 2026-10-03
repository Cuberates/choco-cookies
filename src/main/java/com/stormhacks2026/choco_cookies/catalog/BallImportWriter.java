package com.stormhacks2026.choco_cookies.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BallImportWriter {
    private final BowlingBallRepository repository;
    public BallImportWriter(BowlingBallRepository repository) { this.repository = repository; }
    @Transactional
    public boolean upsert(BallData data) {
        // Serializes cooperating imports across processes; uniqueness is also enforced by PostgreSQL.
        repositoryLock();
        var existing = repository.findBySourceKey(data.sourceKey());
        BowlingBall ball = existing.orElseGet(BowlingBall::new);
        ball.update(data);
        repository.saveAndFlush(ball);
        return existing.isEmpty();
    }
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    private void repositoryLock() {
        entityManager.createNativeQuery("SELECT 1 FROM pg_advisory_xact_lock(20462026)").getSingleResult();
    }
}
