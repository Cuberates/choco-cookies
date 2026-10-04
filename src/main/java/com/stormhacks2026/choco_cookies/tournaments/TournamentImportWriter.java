package com.stormhacks2026.choco_cookies.tournaments;

import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TournamentImportWriter {
    private final TournamentRepository repository;
    private final JdbcTemplate jdbc;
    public TournamentImportWriter(TournamentRepository repository, JdbcTemplate jdbc) {
        this.repository = repository; this.jdbc = jdbc;
    }
    @Transactional
    public boolean upsert(TournamentData data) {
        // Production PostgreSQL imports share a transaction lock; H2 fixtures use the unique constraint.
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            if ("PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
                try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(20462027)")) {
                    statement.execute();
                }
            }
            return null;
        });
        var existing = repository.findBySourceId(data.sourceId());
        var tournament = existing.orElseGet(Tournament::new);
        tournament.update(data);
        repository.saveAndFlush(tournament);
        return existing.isEmpty();
    }
}
