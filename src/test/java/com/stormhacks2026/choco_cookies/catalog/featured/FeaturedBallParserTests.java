package com.stormhacks2026.choco_cookies.catalog.featured;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FeaturedBallParserTests {
    final FeaturedBallParser parser = new FeaturedBallParser();
    final String url = FeaturedBallParser.brandUrl("storm") + "/phaze-crimson";
    String fixture(String name) throws Exception {
        try (var stream = getClass().getResourceAsStream("/fixtures/bowwwl/" + name)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    @Test void listingFiltersBrandLinksCoreDirectoriesDuplicatesAndOtherHosts() throws Exception {
        assertEquals(List.of(url, FeaturedBallParser.brandUrl("storm") + "/ion-max"), parser.candidates(fixture("featured-brand.html"), "storm"));
        assertThrows(IllegalArgumentException.class, () -> parser.candidates("<h1>Error</h1>", "storm"));
        assertThrows(IllegalArgumentException.class, () -> FeaturedBallParser.brandUrl("../storm"));
    }
    @Test void liveMarkupSelectsTheBallAndItsCoreInsteadOfLogosOrSimilarBalls() throws Exception {
        var detail = parser.detail(fixture("featured-ball.html"), url, "storm");
        assertEquals("Phaze Crimson", detail.name());
        assertEquals(url, detail.sourceUrl());
        assertTrue(detail.ballImageUrl().endsWith("storm-phaze-crimson.png?itok=mNI2S-it"));
        assertTrue(detail.coreImageUrl().endsWith("storm-velocity-ai-core.png?itok=YpQv0tgH"));
        assertEquals("Velocity A.I. Core", detail.coreName());
        assertEquals("Pearl Reactive", detail.coverstockType());
        assertEquals("Symmetric", detail.coreType());
    }
    @Test void relativeImageUrlsBecomeAbsoluteWithoutDroppingQueryTokens() throws Exception {
        String html = fixture("featured-ball.html").replace("https://www.bowwwl.com/sites/", "/sites/");
        var detail = parser.detail(html, url, "storm");
        assertTrue(detail.ballImageUrl().startsWith("https://www.bowwwl.com/sites/"));
        assertTrue(detail.coreImageUrl().contains("?itok=YpQv0tgH"));
    }
    @Test void missingOrUnsafeImagesAndCrossBrandCanonicalsAreRejected() throws Exception {
        String html = fixture("featured-ball.html");
        assertThrows(IllegalArgumentException.class, () -> parser.detail(html.replace("field--name-field-core-image", "missing-core"), url, "storm"));
        for (String host : List.of("https://example.com/sites/", "http://www.bowwwl.com/sites/", "https://www.bowwwl.com:443/sites/", "https://user@www.bowwwl.com/sites/")) {
            String unsafe = html.replace("https://www.bowwwl.com/sites/", host);
            assertThrows(IllegalArgumentException.class, () -> parser.detail(unsafe, url, "storm"), host);
        }
        assertThrows(IllegalArgumentException.class, () -> parser.detail(html.replace(url, FeaturedBallParser.brandUrl("ebonite") + "/spartan"), url, "storm"));
        assertThrows(IllegalArgumentException.class, () -> parser.detail(html, url, "brunswick"));
    }
    @Test void optionalSpecificationsCanBeMissingWithoutInventingValues() throws Exception {
        String html = fixture("featured-ball.html").replace("field--name-field-coverstock-type", "missing-cover").replace("field--name-field-core-type", "missing-type");
        var detail = parser.detail(html, url, "storm");
        assertNull(detail.coverstockType()); assertNull(detail.coreType());
    }
}
