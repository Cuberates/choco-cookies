package com.stormhacks2026.choco_cookies.tournaments;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tournaments.import", havingValue = "true")
public class TournamentImportCommand implements ApplicationRunner {
    private final TournamentImporter importer;
    private final int maxEvents;
    public TournamentImportCommand(TournamentImporter importer, @Value("${tournaments.import.max-events:25}") int maxEvents) {
        this.importer = importer; this.maxEvents = maxEvents;
    }
    @Override public void run(ApplicationArguments args) throws Exception {
        var ids = args.getOptionValues("tournaments.import.source-id");
        if (args.containsOption("tournaments.import.source-id") && (ids == null || ids.size() != 1))
            throw new IllegalArgumentException("Supply exactly one tournaments.import.source-id");
        var report = ids == null ? importer.run(maxEvents) : importer.runSingle(Long.parseLong(ids.get(0)));
        System.out.printf("Tournament import: inserted=%d updated=%d skipped=%d failed=%d%n",
                report.inserted(), report.updated(), report.skipped(), report.failed());
        report.errors().forEach(System.err::println);
    }
}
