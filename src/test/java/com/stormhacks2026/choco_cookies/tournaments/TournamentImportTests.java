package com.stormhacks2026.choco_cookies.tournaments;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TournamentImportTests {
    @Test void boundedImportDeduplicatesAndContinuesAfterBadDetails() throws Exception {
        var fetcher = mock(TournamentBowlFetcher.class); var writer = mock(TournamentImportWriter.class);
        var parser = new TournamentBowlParser(); var importer = new TournamentImporter(fetcher, parser, writer);
        String listing = TournamentParserTests.fixture("listing.html");
        listing = listing.replace("</table>", "<tr><td><a href='TournamentHome.cfm?ID_Tournament=35497'>Duplicate</a></td></tr></table>");
        when(fetcher.fetch(TournamentBowlParser.UPCOMING)).thenReturn(listing);
        when(fetcher.fetch(TournamentBowlParser.detailUrl(27638))).thenThrow(new IOException("HTTP 404"));
        when(fetcher.fetch(TournamentBowlParser.detailUrl(35497))).thenReturn(TournamentParserTests.fixture("detail-35497.html"));
        when(fetcher.fetch(TournamentBowlParser.detailUrl(36040))).thenReturn(TournamentParserTests.fixture("detail-36040.html"));
        when(writer.upsert(any())).thenReturn(true,true,false,false);
        var first = importer.run(25);
        assertEquals(2, first.inserted()); assertEquals(1, first.failed()); assertEquals(1, first.skipped());
        assertTrue(first.errors().get(0).contains("HTTP 404"));
        var second = importer.run(25); assertEquals(2, second.updated());
        verify(fetcher, times(2)).fetch(TournamentBowlParser.detailUrl(35497));
        verify(writer, times(4)).upsert(any());
        assertThrows(IllegalArgumentException.class, () -> importer.run(0));
        assertThrows(IllegalArgumentException.class, () -> importer.run(1001));
    }
    @Test void capCountsFailedAttemptsAndSingleImportDoesNotFetchListing() throws Exception {
        var fetcher = mock(TournamentBowlFetcher.class); var writer = mock(TournamentImportWriter.class);
        var importer = new TournamentImporter(fetcher, new TournamentBowlParser(), writer);
        when(fetcher.fetch(TournamentBowlParser.UPCOMING)).thenReturn(TournamentParserTests.fixture("listing.html"));
        when(fetcher.fetch(TournamentBowlParser.detailUrl(27638))).thenReturn("<h1>Broken</h1>");
        assertEquals(1, importer.run(1).failed()); verifyNoInteractions(writer);
        verify(fetcher, never()).fetch(TournamentBowlParser.detailUrl(35497));
        clearInvocations(fetcher);
        when(fetcher.fetch(TournamentBowlParser.detailUrl(35497))).thenReturn(TournamentParserTests.fixture("detail-35497.html"));
        when(writer.upsert(any())).thenReturn(true);
        assertEquals(1, importer.runSingle(35497).inserted());
        verify(fetcher).fetch(TournamentBowlParser.detailUrl(35497)); verifyNoMoreInteractions(fetcher);
    }
}
