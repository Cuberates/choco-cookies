package com.stormhacks2026.choco_cookies.catalog;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;

class CatalogImportTests {
    private final BowwwlParser parser = new BowwwlParser();
    private String fixture(String name) throws Exception {
        try (var stream = getClass().getResourceAsStream("/fixtures/bowwwl/" + name)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    @Test void parsesMultipleWeightsAndOptionalFields() throws Exception {
        var data = parser.parseBall(fixture("ball.html"), BowwwlParser.ROOT + "/test/sample?tracking=1");
        assertEquals(Set.of(12, 15, 16), data.weights());
        assertEquals("Test Brand", data.brand());
        assertEquals("Pearl Reactive", data.coverstockType());
        assertEquals("Symmetric", data.coreType());
        assertEquals(BowwwlParser.ROOT + "/test/sample", data.sourceKey());
        assertNull(data.sourceId());
        var sparse = parser.parseBall("<h1><span class='field--name-title'>Sparse</span></h1>", BowwwlParser.ROOT + "/test/sparse");
        assertNull(sparse.brand()); assertNull(sparse.coreType()); assertNull(sparse.coverstockType());
        assertTrue(sparse.weights().isEmpty());
    }
    @Test void rejectsMalformedPagesAndWeights() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> parser.parseBall("<h1>Error</h1>", BowwwlParser.ROOT));
        String badWeights = fixture("ball.html").replace("16 pounds", "12-16 pounds");
        assertThrows(IllegalArgumentException.class, () -> parser.parseBall(badWeights, BowwwlParser.ROOT));
        assertThrows(IllegalArgumentException.class, () -> parser.parseCatalog("<html></html>", BowwwlParser.ROOT));
        assertThrows(IllegalArgumentException.class, () -> BowwwlParser.normalize("https://example.com/bowling-ball-database"));
    }
    @Test void constructsAndImportsOneBallWithoutFetchingListings() throws Exception {
        String url = BowwwlParser.ROOT + "/storm/phaze-ii";
        assertEquals(url, CatalogImporter.ballUrl(" Storm ", "Phaze II"));
        assertEquals(url, CatalogImporter.ballUrl("storm", "phaze-ii"));
        assertThrows(IllegalArgumentException.class, () -> CatalogImporter.ballUrl("../storm", "phaze-ii"));
        assertThrows(IllegalArgumentException.class, () -> CatalogImporter.ballUrl("storm", ""));
        var fetcher = mock(BowwwlFetcher.class);
        var writer = mock(BallImportWriter.class);
        when(fetcher.fetch(url)).thenReturn(fixture("ball.html"));
        when(writer.upsert(any())).thenReturn(true, false);
        var importer = new CatalogImporter(parser, fetcher, writer);
        assertEquals(1, importer.runBall("Storm", "Phaze II").inserted());
        assertEquals(1, importer.runBall("Storm", "Phaze II").updated());
        verify(fetcher, times(2)).fetch(url);
        verifyNoMoreInteractions(fetcher);
        when(fetcher.fetch(url)).thenReturn("<h1>Error</h1>");
        assertEquals(1, importer.runBall("Storm", "Phaze II").malformed());
        verify(writer, times(2)).upsert(any());
    }
    @Test void followsSourcePagination() throws Exception {
        var page = parser.parseCatalog(fixture("catalog.html"), BowwwlParser.ROOT);
        assertEquals(3, page.ballUrls().size());
        assertEquals(BowwwlParser.ROOT + "?weight=&page=1", page.nextUrl());
    }
    @Test void reportsDuplicatesMalformedRowsAndRepeatUpdatesWithoutNetwork() throws Exception {
        var fetcher = mock(BowwwlFetcher.class);
        var writer = mock(BallImportWriter.class);
        String sample = BowwwlParser.ROOT + "/test/sample";
        when(fetcher.fetch(anyString())).thenReturn("<html>Error</html>");
        when(fetcher.fetch(BowwwlParser.ROOT + "?weight=")).thenReturn(fixture("catalog.html"));
        when(fetcher.fetch(sample)).thenReturn(fixture("ball.html"));
        when(writer.upsert(any())).thenReturn(true, false);
        var importer = new CatalogImporter(parser, fetcher, writer);
        var first = importer.run(1);
        assertEquals(1, first.inserted()); assertEquals(0, first.updated());
        assertEquals(1, first.skipped()); assertEquals(1, first.malformed());
        assertTrue(first.errors().get(0).contains("Ball title missing"));
        var second = importer.run(1);
        assertEquals(0, second.inserted()); assertEquals(1, second.updated());
        verify(writer, times(2)).upsert(any());
    }
}
