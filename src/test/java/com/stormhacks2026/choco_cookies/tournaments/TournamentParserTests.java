package com.stormhacks2026.choco_cookies.tournaments;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TournamentParserTests {
    private final TournamentBowlParser parser = new TournamentBowlParser();
    static String fixture(String name) throws Exception {
        try (var stream = TournamentParserTests.class.getResourceAsStream("/fixtures/tournamentbowl/" + name)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private TournamentData detail(long id) throws Exception {
        var listing = parser.parseListing(fixture("listing.html")).stream().filter(row -> row.sourceId() == id).findFirst().orElseThrow();
        return parser.parseDetail(fixture("detail-" + id + ".html"), listing);
    }
    @Test void extractsActualListingAndDetailMarkupAndPreservesNamedFees() throws Exception {
        var listing = parser.parseListing(fixture("listing.html"));
        assertEquals(3, listing.size());
        assertEquals("Bowlarena Lanes", listing.get(0).venueName());
        assertEquals("Jacksonville", listing.get(0).city());
        assertEquals("NC", listing.get(0).region());
        var event = detail(36040);
        assertEquals("October - Maple Lanes Shootout", event.title());
        assertEquals("Maple Lanes Countryside", event.venueName());
        assertEquals("Clearwater", event.city()); assertEquals("FL", event.region());
        assertEquals("27867 Highway 19 N\nClearwater, FL", event.address());
        assertEquals(LocalDate.of(2026, 10, 3), event.startDate()); assertEquals(event.startDate(), event.endDate());
        assertEquals(8, event.entryFees().size());
        var tournament = new Tournament(); tournament.update(event);
        assertEquals(new BigDecimal("10"), tournament.getEntryPriceFrom()); assertEquals("$", tournament.getCurrencySymbol());
        assertEquals(new BigDecimal("200"), tournament.getEntryFees().get(0).getAmount());
    }
    @Test void dateRangesAndFreeAdvancementDoNotMisrepresentStartingPrice() throws Exception {
        var data = detail(35497);
        assertEquals("41st Annual John Willey Memorial Open", data.title());
        assertEquals("Great Falls", data.city()); assertEquals("MT", data.region());
        assertEquals(LocalDate.of(2026,10,4), data.endDate());
        assertEquals(4, data.entryFees().size());
        var tournament = new Tournament(); tournament.update(data);
        assertEquals(new BigDecimal("175"), tournament.getEntryPriceFrom());
        assertEquals(new BigDecimal("0"), data.entryFees().get(1).getAmount());
    }
    @Test void sparsePlainTextVenueAndInvalidDatesRemainUsable() {
        var row = new TournamentBowlParser.Listing(1L, TournamentBowlParser.detailUrl(1), "Short", "Fallback", "Fallback City", "AB");
        var sparse = parser.parseDetail("<title>Full title</title><div class=card><h2>Welcome</h2>Hosted by<br>Plain Lanes<br>Feb 30,2026<br>Director note: $999 prize fund</div>", row);
        assertEquals("Plain Lanes", sparse.venueName()); assertNull(sparse.startDate());
        assertEquals("Feb 30,2026", sparse.dateText()); assertEquals("Fallback City", sparse.city());
        assertTrue(sparse.entryFees().isEmpty());
        var tournament = new Tournament(); tournament.update(sparse); assertNull(tournament.getEntryPriceFrom());
        var free = parser.parseDetail("<title>Free</title><div class=card><h2>Welcome</h2>Hosted by Venue<br>Free entry - $0 entry</div>",row);
        tournament.update(free); assertEquals(BigDecimal.ZERO, tournament.getEntryPriceFrom());
        var unlisted = parser.parseDetail("<title>Unlisted</title><div class=card><h2>Welcome</h2>Hosted by<br></div>",
                new TournamentBowlParser.Listing(2L,TournamentBowlParser.detailUrl(2),null,null,null,null));
        assertNull(unlisted.venueName()); assertNull(unlisted.startDate());
        var noHost = parser.parseDetail("<title>Missing host</title><div class=card><h2>Welcome to the event</h2>Oct 3,2026</div>", row);
        assertEquals("Fallback", noHost.venueName());
        assertEquals(LocalDate.of(2026, 10, 3), noHost.startDate());
    }
    @Test void canonicalizesIdentityAndRejectsMalformedAndForeignPages() {
        assertEquals(42, TournamentBowlParser.sourceId("https://tournamentbowl.com/open/tournamenthome.cfm?id_tournament=42&tracking=1"));
        for (String url : new String[]{"http://tournamentbowl.com/open/TournamentHome.cfm?ID_Tournament=42",
                "https://evil.test/open/TournamentHome.cfm?ID_Tournament=42", "https://tournamentbowl.com/open/TournamentHome.cfm?ID_Tournament=0",
                "https://tournamentbowl.com/open/TournamentHome.cfm?ID_Tournament=42&id_tournament=43"})
            assertThrows(IllegalArgumentException.class, () -> TournamentBowlParser.sourceId(url));
        assertThrows(IllegalArgumentException.class, () -> parser.parseListing("<h1>Error</h1>"));
        assertThrows(IllegalArgumentException.class, () -> parser.parseDetail("<title>Error</title>", new TournamentBowlParser.Listing(42L,TournamentBowlParser.detailUrl(42),null,null,null,null)));
        assertTrue(parser.parseListing("<table class=card>No upcoming tournaments</table>").isEmpty());
    }
}
