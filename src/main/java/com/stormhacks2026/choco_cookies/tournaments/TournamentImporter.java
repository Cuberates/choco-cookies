package com.stormhacks2026.choco_cookies.tournaments;

import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class TournamentImporter {
    public record ImportReport(int inserted, int updated, int skipped, int failed, List<String> errors) {}
    private final TournamentBowlFetcher fetcher;
    private final TournamentBowlParser parser;
    private final TournamentImportWriter writer;
    public TournamentImporter(TournamentBowlFetcher fetcher, TournamentBowlParser parser, TournamentImportWriter writer) {
        this.fetcher = fetcher; this.parser = parser; this.writer = writer;
    }
    public ImportReport run(int maxEvents) throws IOException, InterruptedException {
        if (maxEvents < 1 || maxEvents > 1000) throw new IllegalArgumentException("max-events must be between 1 and 1000");
        var listing = parser.parseListing(fetcher.fetch(TournamentBowlParser.UPCOMING));
        var seen = new HashSet<Long>();
        var errors = new ArrayList<String>();
        int inserted = 0, updated = 0, skipped = 0, failed = 0;
        for (var item : listing) {
            if (!seen.add(item.sourceId())) { skipped++; continue; }
            if (inserted + updated + failed >= maxEvents) break;
            TournamentData data;
            try { data = parser.parseDetail(fetcher.fetch(item.sourceUrl()), item); }
            catch (IOException | IllegalArgumentException ex) {
                failed++; errors.add(item.sourceUrl() + ": " + ex.getMessage()); continue;
            }
            if (writer.upsert(data)) inserted++; else updated++;
        }
        return new ImportReport(inserted, updated, skipped, failed, List.copyOf(errors));
    }
    public ImportReport runSingle(long sourceId) throws IOException, InterruptedException {
        String url = TournamentBowlParser.detailUrl(sourceId);
        var listing = new TournamentBowlParser.Listing(sourceId, url, null, null, null, null);
        TournamentData data;
        try { data = parser.parseDetail(fetcher.fetch(url), listing); }
        catch (IllegalArgumentException ex) { return new ImportReport(0, 0, 0, 1, List.of(url + ": " + ex.getMessage())); }
        boolean inserted = writer.upsert(data);
        return new ImportReport(inserted ? 1 : 0, inserted ? 0 : 1, 0, 0, List.of());
    }
}
