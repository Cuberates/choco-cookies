package com.stormhacks2026.choco_cookies.catalog.featured;

import com.stormhacks2026.choco_cookies.catalog.BowwwlFetcher;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

class FeaturedCatalogTests {
    final FeaturedBallParser parser = new FeaturedBallParser();
    final BowwwlFetcher fetcher = mock(BowwwlFetcher.class);
    final FeaturedBallRepository repository = mock(FeaturedBallRepository.class);
    final Instant now = Instant.parse("2026-10-04T12:00:00Z");
    final Clock clock = mock(Clock.class);
    FeaturedCatalog service() {
        when(clock.instant()).thenReturn(now);
        return new FeaturedCatalog(fetcher, parser, repository, clock, new Random(4));
    }
    static String listing(String brand, int count) {
        var html = new StringBuilder();
        for (int i = 0; i < count; i++) html.append("<a class='card-link' rel='bookmark' href='")
                .append(FeaturedBallParser.brandUrl(brand)).append("/ball-").append(i).append("'>Ball</a>");
        return html.toString();
    }
    static String detail(String name) {
        return "<h1><span class='field--name-title'>" + name + "</span></h1>"
                + "<div class='field--name-field-ball-image'><img src='/sites/default/files/balls/ball.png?itok=ball'></div>"
                + "<div class='field--name-field-core'><h5><a>Matched core</a></h5>"
                + "<div class='field--name-field-core-image'><img src='/sites/default/files/cores/core.png?itok=core'></div></div>";
    }
    void successfulSource(int candidates) throws Exception {
        when(fetcher.fetch(anyString())).thenAnswer(call -> {
            String url = call.getArgument(0);
            String brand = url.substring("https://www.bowwwl.com/bowling-ball-database/".length()).split("/")[0];
            return url.equals(FeaturedBallParser.brandUrl(brand)) ? listing(brand, candidates) : detail("Random " + brand);
        });
    }
    @Test void orderedBrandSelectionsReadOnlyPersistedDataAndNeverFetch() {
        var featured = service();
        var details = parser.detail(detail("Seed"), FeaturedBallParser.brandUrl("storm") + "/seed", "storm");
        when(repository.findAll()).thenReturn(List.of(new FeaturedBall("brunswick", "Brunswick", details, now), new FeaturedBall("storm", "Storm", details, now)));
        assertEquals(List.of("storm", "brunswick"), featured.selections().stream().map(FeaturedBall::getBrandSlug).toList());
        verifyNoInteractions(fetcher);
    }
    @Test void refreshRandomizesCandidatesSavesFourMatchingPairsAndUsesCooldown() throws Exception {
        var featured = service(); successfulSource(8);
        var saved = new ArrayList<FeaturedBall>();
        when(repository.saveAndFlush(any())).thenAnswer(call -> { FeaturedBall ball = call.getArgument(0); saved.add(ball); return ball; });
        var report = featured.refresh(); assertEquals(4, report.updated()); assertEquals(0, report.failed());
        assertEquals(FeaturedCatalog.BRAND_SLUGS, saved.stream().map(FeaturedBall::getBrandSlug).toList());
        assertTrue(saved.stream().anyMatch(ball -> !ball.getSourceUrl().endsWith("/ball-0")));
        for (var ball : saved) {
            assertTrue(ball.getSourceUrl().startsWith(FeaturedBallParser.brandUrl(ball.getBrandSlug()) + "/"));
            assertTrue(ball.getBallImageUrl().endsWith("ball.png?itok=ball"));
            assertTrue(ball.getCoreImageUrl().endsWith("core.png?itok=core"));
        }
        assertTrue(featured.refresh().limited()); verify(repository, times(4)).saveAndFlush(any());
        when(clock.instant()).thenReturn(now.plusSeconds(300));
        assertEquals(4, featured.refresh().updated());
    }
    @Test void missingPairsRetryOnlyThreeCandidatesAndRetainPriorSelections() throws Exception {
        var featured = service();
        when(fetcher.fetch(anyString())).thenAnswer(call -> {
            String url = call.getArgument(0); String brand = url.substring(FeaturedBallParser.brandUrl("storm").lastIndexOf('/') + 1).split("/")[0];
            return url.endsWith("/" + brand) ? listing(brand, 8) : "<h1><span class='field--name-title'>No core</span></h1>";
        });
        var report = featured.refresh(); assertEquals(0, report.updated()); assertEquals(4, report.failed());
        verify(fetcher, times(16)).fetch(anyString()); verifyNoInteractions(repository);
    }
    @Test void brandFailureDoesNotStopOtherBrandsOrOverwriteTheFailedBrand() throws Exception {
        var featured = service(); successfulSource(1);
        when(fetcher.fetch(FeaturedBallParser.brandUrl("ebonite"))).thenThrow(new IOException("Unavailable"));
        var report = featured.refresh(); assertEquals(3, report.updated()); assertEquals(1, report.failed());
        verify(repository, times(3)).saveAndFlush(any());
        verify(repository, never()).saveAndFlush(argThat(ball -> ball.getBrandSlug().equals("ebonite")));
        assertTrue(report.message().contains("Previous selections"));
    }
    @Test void interruptionStopsRequestsAndRestoresThreadFlag() throws Exception {
        var featured = service();
        when(fetcher.fetch(anyString())).thenThrow(new InterruptedException());
        try { assertEquals(0, featured.refresh().updated()); assertTrue(Thread.currentThread().isInterrupted()); }
        finally { Thread.interrupted(); }
        verify(fetcher, times(1)).fetch(anyString()); verifyNoInteractions(repository);
    }
    @Test void concurrentRefreshIsLimitedWhileFirstRequestIsInFlight() throws Exception {
        var featured = service();
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        when(fetcher.fetch(anyString())).thenAnswer(call -> { entered.countDown(); if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("Timed out"); throw new IOException("Unavailable"); });
        var executor = Executors.newSingleThreadExecutor();
        try {
            var running = executor.submit(featured::refresh);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertTrue(featured.refresh().limited()); release.countDown();
            assertEquals(0, running.get(5, TimeUnit.SECONDS).updated());
        } finally { release.countDown(); executor.shutdownNow(); }
    }
}
