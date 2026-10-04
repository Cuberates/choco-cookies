package com.stormhacks2026.choco_cookies.catalog.featured;

import com.stormhacks2026.choco_cookies.catalog.BowwwlFetcher;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class FeaturedCatalog {
    public static final List<String> BRAND_SLUGS = List.of("storm", "ebonite", "900-global", "brunswick");
    private static final Map<String, String> BRAND_NAMES = Map.of("storm", "Storm", "ebonite", "Ebonite", "900-global", "900 Global", "brunswick", "Brunswick");
    public record RefreshReport(int updated, int failed, boolean limited) {
        public String message() {
            if (limited) return "Featured balls were recently refreshed. Please try again in five minutes.";
            if (updated == 4) return "Four featured balls refreshed from Bowwwl.";
            if (updated == 0) return "Could not refresh featured balls. The previous selections are still available.";
            return updated + " featured balls refreshed. Previous selections were kept for the remaining brands.";
        }
    }
    private final BowwwlFetcher fetcher;
    private final FeaturedBallParser parser;
    private final FeaturedBallRepository repository;
    private final Clock clock;
    private final Random random;
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private volatile Instant nextRefresh = Instant.MIN;

    @Autowired public FeaturedCatalog(BowwwlFetcher fetcher, FeaturedBallParser parser, FeaturedBallRepository repository) {
        this(fetcher, parser, repository, Clock.systemUTC(), new Random());
    }
    FeaturedCatalog(BowwwlFetcher fetcher, FeaturedBallParser parser, FeaturedBallRepository repository, Clock clock, Random random) {
        this.fetcher = fetcher; this.parser = parser; this.repository = repository; this.clock = clock; this.random = random;
    }
    public List<FeaturedBall> selections() {
        var byBrand = new HashMap<String, FeaturedBall>();
        repository.findAll().forEach(ball -> byBrand.put(ball.getBrandSlug(), ball));
        return BRAND_SLUGS.stream().map(byBrand::get).filter(Objects::nonNull).toList();
    }
    public RefreshReport refresh() {
        if (!refreshing.compareAndSet(false, true)) return new RefreshReport(0, 0, true);
        try {
            Instant now = clock.instant();
            if (now.isBefore(nextRefresh)) return new RefreshReport(0, 0, true);
            nextRefresh = now.plus(Duration.ofMinutes(5));
            int updated = 0, failed = 0;
            for (String brand : BRAND_SLUGS) {
                try {
                    var candidates = new ArrayList<>(parser.candidates(fetcher.fetch(FeaturedBallParser.brandUrl(brand)), brand));
                    Collections.shuffle(candidates, random);
                    boolean saved = false;
                    for (String url : candidates.stream().limit(3).toList()) {
                        try {
                            var detail = parser.detail(fetcher.fetch(url), url, brand);
                            repository.saveAndFlush(new FeaturedBall(brand, BRAND_NAMES.get(brand), detail, clock.instant()));
                            saved = true;
                            break;
                        } catch (IOException | IllegalArgumentException ex) { /* Try another eligible candidate. */ }
                    }
                    if (saved) updated++; else failed++;
                } catch (IOException | IllegalArgumentException | DataAccessException ex) {
                    failed++;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return new RefreshReport(updated, 4 - updated, false);
                }
            }
            return new RefreshReport(updated, failed, false);
        } finally { refreshing.set(false); }
    }
}
