package com.stormhacks2026.choco_cookies.tournaments;

import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface TournamentRepository extends JpaRepository<Tournament, Long>, JpaSpecificationExecutor<Tournament> {
    Optional<Tournament> findBySourceId(Long sourceId);
}
