package com.stormhacks2026.choco_cookies.catalog.featured;

import com.stormhacks2026.choco_cookies.catalog.BowwwlFetcher;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.config.import=", "spring.datasource.url=jdbc:h2:mem:featured;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate", "catalog.import=false", "tournaments.import=false", "catalog.featured.refresh=false"})
@Transactional
class FeaturedCatalogPageTests {
    @Autowired WebApplicationContext context;
    @Autowired SessionRepositoryFilter<?> sessionFilter;
    @Autowired FeaturedBallRepository repository;
    @MockitoBean BowwwlFetcher fetcher;
    MockMvc mvc;
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(sessionFilter).apply(springSecurity()).build(); }
    @Test void seedsAndRendersFourOrderedCardsWithAdjacentImagesWithoutNetwork() throws Exception {
        assertEquals(4, repository.count());
        var response = mvc.perform(get("/catalog")).andExpect(status().isOk()).andReturn().getResponse();
        var doc = Jsoup.parse(response.getContentAsString());
        var cards = doc.select(".featured-ball-card"); assertEquals(4, cards.size());
        assertEquals(java.util.List.of("Storm", "Ebonite", "900 Global", "Brunswick"), cards.stream().map(card -> card.attr("data-brand")).toList());
        for (var card : cards) {
            var figures = card.select(".featured-image-pair > figure"); assertEquals(2, figures.size());
            assertEquals("Ball", figures.get(0).selectFirst("figcaption").text());
            assertEquals("Core", figures.get(1).selectFirst("figcaption").text());
            assertTrue(figures.get(0).selectFirst("img").attr("src").contains("/balls/"));
            assertTrue(figures.get(1).selectFirst("img").attr("src").contains("/cores/"));
            assertFalse(card.selectFirst("img").attr("alt").isBlank());
            assertEquals("noopener noreferrer", card.selectFirst(".source-link").attr("rel"));
        }
        assertNotNull(doc.selectFirst("form[action='/catalog/featured/refresh'] input[name=_csrf]"));
        assertTrue(doc.select(".hero-image").attr("src").contains("phaze-ii.png"));
        assertFalse(doc.select("section.filter-panel").isEmpty());
        assertEquals(0, doc.select("details.filter-panel").size());
        verifyNoInteractions(fetcher);
    }
    @Test void refreshRequiresPostAndCsrfAndPersistsNewSelections() throws Exception {
        mvc.perform(post("/catalog/featured/refresh")).andExpect(status().isForbidden());
        mvc.perform(get("/catalog/featured/refresh")).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(fetcher);
        when(fetcher.fetch(anyString())).thenAnswer(call -> {
            String url = call.getArgument(0);
            for (String brand : FeaturedCatalog.BRAND_SLUGS) {
                if (url.equals(FeaturedBallParser.brandUrl(brand))) return FeaturedCatalogTests.listing(brand, 1);
            }
            return FeaturedCatalogTests.detail("New selection");
        });
        mvc.perform(post("/catalog/featured/refresh").with(csrf())).andExpect(redirectedUrl("/catalog#featured-showcase"))
                .andExpect(flash().attribute("featuredMessage", "Four featured balls refreshed from Bowwwl."));
        assertEquals(4, repository.count());
        assertTrue(repository.findAll().stream().allMatch(ball -> ball.getName().equals("New selection")));
        mvc.perform(post("/catalog/featured/refresh").with(csrf())).andExpect(redirectedUrl("/catalog#featured-showcase"))
                .andExpect(flash().attribute("featuredMessage", "Featured balls were recently refreshed. Please try again in five minutes."));
        verify(fetcher, times(8)).fetch(anyString());
    }
}
