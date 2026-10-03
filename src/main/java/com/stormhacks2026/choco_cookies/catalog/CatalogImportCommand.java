package com.stormhacks2026.choco_cookies.catalog;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "catalog.import", havingValue = "true")
public class CatalogImportCommand implements ApplicationRunner {
    private final CatalogImporter importer;
    private final int maxPages;
    public CatalogImportCommand(CatalogImporter importer, @Value("${catalog.import.max-pages:1}") int maxPages) {
        this.importer = importer; this.maxPages = maxPages;
    }
    @Override public void run(ApplicationArguments args) throws Exception {
        boolean single = args.containsOption("catalog.import.brand") || args.containsOption("catalog.import.name");
        var report = single ? importer.runBall(option(args, "catalog.import.brand"),
                option(args, "catalog.import.name")) : importer.run(maxPages);
        System.out.printf("Catalog import: inserted=%d updated=%d skipped=%d malformed=%d%n",
            report.inserted(), report.updated(), report.skipped(), report.malformed());
        report.errors().forEach(System.err::println);
    }
    private static String option(ApplicationArguments args, String key) {
        var values = args.getOptionValues(key);
        if (values == null || values.size() != 1 || values.get(0).isBlank())
            throw new IllegalArgumentException("Single-ball import requires exactly one --" + key + " value");
        return values.get(0);
    }
}
