package com.stormhacks2026.choco_cookies.catalog;

import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class CatalogImporter {
    public record ImportReport(int inserted, int updated, int skipped, int malformed, List<String> errors) {}
    private final BowwwlParser parser;
    private final BowwwlFetcher fetcher;
    private final BallImportWriter writer;
    public CatalogImporter(BowwwlParser parser, BowwwlFetcher fetcher, BallImportWriter writer) {
        this.parser = parser; this.fetcher = fetcher; this.writer = writer;
    }
    public static String ballUrl(String brand, String name) {
        return BowwwlParser.ROOT + "/" + slug(brand) + "/" + slug(name);
    }
    private static String slug(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Brand and name are required");
        String slug = value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
        if (!slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*"))
            throw new IllegalArgumentException("Use a Bowwwl slug or a name containing letters, numbers, spaces, and hyphens");
        return slug;
    }
    public ImportReport runBall(String brand, String name) throws IOException, InterruptedException {
        String url = ballUrl(brand, name);
        String html = fetcher.fetch(url);
        BallData data;
        try { data = parser.parseBall(html, url); }
        catch (IllegalArgumentException ex) {
            return new ImportReport(0, 0, 0, 1, List.of(url + ": " + ex.getMessage()));
        }
        boolean inserted = writer.upsert(data);
        return new ImportReport(inserted ? 1 : 0, inserted ? 0 : 1, 0, 0, List.of());
    }
    public ImportReport run(int maxPages) throws IOException, InterruptedException {
        if (maxPages < 1 || maxPages > 1000) throw new IllegalArgumentException("max-pages must be between 1 and 1000");
        int inserted = 0, updated = 0, skipped = 0, malformed = 0;
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>(), pages = new HashSet<>();
        String pageUrl = BowwwlParser.ROOT + "?weight=";
        for (int page = 0; page < maxPages && pageUrl != null; page++) {
            if (!pages.add(pageUrl)) throw new IOException("Repeated catalog pagination URL");
            var catalog = parser.parseCatalog(fetcher.fetch(pageUrl), pageUrl);
            for (String url : catalog.ballUrls()) {
                if (!seen.add(url)) { skipped++; continue; }
                String html = fetcher.fetch(url);
                BallData data;
                try { data = parser.parseBall(html, url); }
                catch (IllegalArgumentException ex) {
                    malformed++; errors.add(url + ": " + ex.getMessage()); continue;
                }
                if (!data.sourceKey().equals(url) && !seen.add(data.sourceKey())) { skipped++; continue; }
                // Database/network failures abort the command rather than being called malformed input.
                if (writer.upsert(data)) inserted++; else updated++;
            }
            pageUrl = catalog.nextUrl();
        }
        return new ImportReport(inserted, updated, skipped, malformed, List.copyOf(errors));
    }
}
