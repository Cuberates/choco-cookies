package com.stormhacks2026.choco_cookies.catalog;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties = {"spring.config.import=", "spring.datasource.url=jdbc:h2:mem:catalog;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "catalog.import=false"})
@Transactional
class CatalogSearchTests {
    @Autowired BowlingBallRepository repository;
    @Autowired WebApplicationContext context;

    void ball(String name, String brand, String coverstock, String core, Set<Integer> weights) {
        var ball = new BowlingBall();
        ball.update(new BallData("https://example.com/" + name, "https://example.com/" + name,
                null, name, brand, coverstock, core, weights));
        repository.saveAndFlush(ball);
    }
    long count(String name, Integer weight, String coverstock, String core, String brand) {
        return repository.count(CatalogController.filters(name, weight, coverstock, core, brand));
    }
    @Test void filtersCombineAndTreatSearchAsLiteralCaseInsensitiveText() {
        ball("Storm Alpha", "Storm", "Solid", "Symmetric", Set.of(12, 15));
        ball("Storm Beta", "Other", "Pearl", "Asymmetric", Set.of(14));
        ball("100%_Ball", null, null, null, Set.of());
        assertEquals(2, count("sToRm", null, "", "", ""));
        assertEquals(1, count("other", null, "", "", ""));
        assertEquals(1, count("pearl", null, "", "", ""));
        assertEquals(1, count("asymmetric", null, "", "", ""));
        assertEquals(1, count("", 12, "", "", ""));
        assertEquals(1, count("", null, "Solid", "", ""));
        assertEquals(1, count("", null, "", "Symmetric", ""));
        assertEquals(1, count("", null, "", "", "Storm"));
        assertEquals(1, count("alpha", 15, "Solid", "Symmetric", "Storm"));
        assertEquals(0, count("alpha", 14, "Solid", "Symmetric", "Storm"));
        assertEquals(0, count("", null, "solid", "", ""));
        assertEquals(1, count("%_", null, "", "", ""));
        assertEquals(java.util.List.of(12, 14, 15), repository.findWeights());
        assertEquals(java.util.List.of("Other", "Storm"), repository.findBrands());
        assertEquals(java.util.List.of("Pearl", "Solid"), repository.findCoverstockTypes());
        assertEquals(java.util.List.of("Asymmetric", "Symmetric"), repository.findCoreTypes());
    }
    @Test void routesRenderSelectionsPaginationUnknownValuesAndEmptyState() throws Exception {
        for (int i = 0; i < 21; i++) ball(String.format("Ball %02d", i), "Brand", "Solid", "Symmetric", Set.of(15));
        ball("Unknown Ball", null, null, null, Set.of());
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/catalog"));
        mvc.perform(get("/catalog").param("name", "Ball").param("brand", "Brand")
                .param("weight", "15").param("coverstockType", "Solid").param("coreType", "Symmetric"))
                .andExpect(status().isOk()).andExpect(view().name("catalog"))
                .andExpect(content().string(containsString("action=\"/catalog/search\"")))
                .andExpect(content().string(containsString("Find your next")))
                .andExpect(content().string(containsString("/assets/phaze-ii.png")))
                .andExpect(content().string(containsString("/css/catalog.css")))
                .andExpect(content().string(containsString("id=\"catalog-results\" open=\"open\"")))
                .andExpect(model().attribute("catalogCount", 22L))
                .andExpect(content().string(containsString("21 balls found")))
                .andExpect(content().string(containsString("Page 1 of 2")))
                .andExpect(content().string(containsString("value=\"Brand\"")))
                .andExpect(content().string(containsString("page=1")))
                .andExpect(content().string(containsString("weight=15")));
        mvc.perform(get("/catalog").param("brand", "Brand").param("page", "1"))
                .andExpect(content().string(containsString("Ball 20")))
                .andExpect(content().string(containsString("Page 2 of 2")));
        mvc.perform(get("/catalog").param("sort", "name-desc").param("brand", "Brand"))
                .andExpect(content().string(containsString("Ball 20")));
        mvc.perform(get("/catalog").param("name", "Unknown"))
                .andExpect(content().string(containsString("Unspecified")));
        mvc.perform(get("/catalog").param("name", "missing"))
                .andExpect(content().string(containsString("No bowling balls match")))
                .andExpect(content().string(containsString("Clear filters")));
        mvc.perform(get("/catalog").param("page", "-1").param("sort", "invalid"))
                .andExpect(status().isOk()).andExpect(model().attribute("sort", "name"));
        mvc.perform(get("/catalog").param("brand", "Brand").param("page", "999"))
                .andExpect(content().string(containsString("Page 2 of 2")));
        mvc.perform(get("/catalog").param("weight", "invalid")).andExpect(status().isBadRequest());
    }
}
