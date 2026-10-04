package com.stormhacks2026.choco_cookies.tournaments;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = {"spring.config.import=", "spring.datasource.url=jdbc:h2:mem:tournaments;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate", "catalog.import=false", "tournaments.import=false"})
@Transactional
class TournamentPageTests {
    @Autowired TournamentImportWriter writer;
    @Autowired TournamentRepository repository;
    @Autowired WebApplicationContext context;
    @MockitoBean TournamentBowlFetcher fetcher;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entityManager;
    TournamentData event(long id, String title, String venue, String city, String region, LocalDate start, LocalDate end) {
        return new TournamentData(id,TournamentBowlParser.detailUrl(id),title,venue,city,region,null,start,end,null,List.of());
    }
    @Test void migrationAndUpsertRetainIdentityAndReplacePricesAndMissingFields() {
        var first = event(1,"First","Venue","City","BC",LocalDate.now(),LocalDate.now());
        assertTrue(writer.upsert(first)); Long id = repository.findBySourceId(1L).orElseThrow().getId();
        var changed = new TournamentData(1L,first.sourceUrl(),"Changed",null,null,null,null,null,null,null,
                List.of(new TournamentEntryFee("Qualifying",new BigDecimal("175"),"$"), new TournamentEntryFee("Finals",BigDecimal.ZERO,"$")));
        assertFalse(writer.upsert(changed)); assertFalse(writer.upsert(changed));
        entityManager.flush(); entityManager.clear();
        var stored = repository.findBySourceId(1L).orElseThrow();
        assertEquals(id,stored.getId()); assertEquals(1,repository.count()); assertNull(stored.getVenueName());
        assertEquals(2,stored.getEntryFees().size()); assertEquals(new BigDecimal("175.00"),stored.getEntryPriceFrom());
        assertNotNull(stored.getImportedAt());
        writer.upsert(first); entityManager.clear(); assertTrue(repository.findBySourceId(1L).orElseThrow().getEntryFees().isEmpty());
    }
    @Test void filtersUpcomingOverlapLiteralTextAndNullDates() {
        var today = LocalDate.now();
        writer.upsert(event(1,"100%_Open","Shared Lanes","City One","BC",today.minusDays(1),today.plusDays(2)));
        writer.upsert(event(2,"Other","Shared Lanes","City Two","WA",today.plusDays(5),today.plusDays(5)));
        writer.upsert(event(3,"Expired","Venue","Old City","BC",today.minusDays(2),today.minusDays(1)));
        writer.upsert(event(4,"Unknown",null,null,null,null,null));
        assertEquals(3,count("","","",null,false,null,null));
        assertEquals(1,count("%_","","",null,false,null,null));
        assertEquals(1,count("city one","Shared Lanes","BC",null,false,today,today.plusDays(1)));
        assertEquals(1,count("","","",null,true,null,null));
        assertEquals(0,count("unknown","","",null,false,today,null));
        assertEquals(1,count("","Shared Lanes","","City Two",false,null,null));
        assertEquals(1,count("","shared lanes","wa","city two",false,null,null));
    }
    long count(String q,String venue,String region,String city,boolean missing,LocalDate from,LocalDate to) {
        return repository.count(TournamentController.filters(q,venue,region,city,missing,from,to,LocalDate.now()));
    }
    @Test void pagesRenderPaginationLocationsSourceLinksEscapingAndEmptyStates() throws Exception {
        var today=LocalDate.now();
        for(int i=0;i<21;i++) writer.upsert(event(i+1,"Event "+i,"Shared Lanes","City One","BC",today.plusDays(i),today.plusDays(i)));
        writer.upsert(event(22,"<script>unsafe</script>","Shared Lanes","City Two","BC",today.plusDays(1),today.plusDays(1)));
        writer.upsert(event(23,"Unknown",null,null,null,null,null));
        writer.upsert(event(24,"Expired","Old Venue","Old City","BC",today.minusDays(2),today.minusDays(1)));
        var mvc=MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/tournaments")).andExpect(status().isOk()).andExpect(view().name("tournaments"))
                .andExpect(content().string(containsString("23 tournaments found")))
                .andExpect(content().string(containsString("Page 1 of 2")))
                .andExpect(content().string(containsString("&lt;script&gt;unsafe&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("Old Venue"))))
                .andExpect(content().string(containsString("tournamentbowl.com/open/TournamentHome.cfm?ID_Tournament=")));
        mvc.perform(get("/tournaments").param("page",Integer.toString(Integer.MAX_VALUE))).andExpect(content().string(containsString("Page 2 of 2")))
                .andExpect(content().string(containsString("Date not listed"))).andExpect(content().string(containsString("Entry price not listed")));
        mvc.perform(get("/tournaments").param("q","absent")).andExpect(content().string(containsString("No tournaments match")));
        mvc.perform(get("/tournaments").param("q","%_")).andExpect(content().string(containsString("0 tournaments found")));
        mvc.perform(get("/locations")).andExpect(status().isOk()).andExpect(content().string(containsString("3 locations found")))
                .andExpect(content().string(containsString("21 upcoming / ongoing events")))
                .andExpect(content().string(containsString("city=City%20Two")));
        mvc.perform(get("/locations").param("region","BC")).andExpect(content().string(containsString("2 locations found")));
        mvc.perform(get("/locations").param("q","absent")).andExpect(content().string(containsString("No locations match")));
        mvc.perform(get("/tournaments").param("from",today.plusDays(3).toString()).param("to",today.toString())).andExpect(status().isBadRequest());
        mvc.perform(get("/tournaments").param("from","invalid")).andExpect(status().isBadRequest());
        mvc.perform(get("/tournaments").param("sort","invalid").param("page","-1")).andExpect(status().isOk()).andExpect(model().attribute("sort","date"));
        var latest = mvc.perform(get("/tournaments").param("sort","date-desc")).andReturn().getResponse().getContentAsString();
        assertTrue(latest.indexOf("Event 20</h3>") < latest.indexOf("Event 19</h3>"));
        verifyNoInteractions(fetcher);
    }
    @Test void locationPaginationClampsPagesAndPreservesFilters() throws Exception {
        for (int i = 0; i < 21; i++) writer.upsert(event(i + 1, "Open", String.format("Venue %02d", i), "City", "BC", LocalDate.now(), LocalDate.now()));
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/locations").param("region", "BC").param("q", "Venue"))
                .andExpect(content().string(containsString("Page 1 of 2")))
                .andExpect(content().string(containsString("region=BC")))
                .andExpect(content().string(containsString("q=Venue")));
        mvc.perform(get("/locations").param("page", Integer.toString(Integer.MAX_VALUE)))
                .andExpect(content().string(containsString("Page 2 of 2")))
                .andExpect(content().string(containsString("Venue 20")));
        verifyNoInteractions(fetcher);
    }
}
