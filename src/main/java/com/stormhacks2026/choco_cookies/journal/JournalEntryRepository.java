package com.stormhacks2026.choco_cookies.journal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    @EntityGraph(attributePaths = "balls")
    List<JournalEntry> findByUserIdOrderByBowlingDateDescIdDesc(Long userId);
    @EntityGraph(attributePaths = "balls")
    Optional<JournalEntry> findByIdAndUserId(Long id, Long userId);
}
