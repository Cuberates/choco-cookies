package com.stormhacks2026.choco_cookies.catalog;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

class CatalogLookupTests {
    BowlingBallRepository repository;
    CatalogImporter importer;
    MockMvc mvc;
    @BeforeEach void setup() {
        repository = mock(BowlingBallRepository.class);
        importer = mock(CatalogImporter.class);
        mvc = MockMvcBuilders.standaloneSetup(new CatalogController(repository, importer)).build();
    }
    BowlingBall ball() {
        var ball = new BowlingBall();
        String url = CatalogImporter.ballUrl("Storm", "Phaze II");
        ball.update(new BallData(url, url, null, "Phaze II", "Storm", "Solid", "Symmetric", Set.of(15)));
        return ball;
    }
    @Test void localMatchesDoNotFetchEvenWhenOtherFiltersExcludeThem() throws Exception {
        when(repository.exists(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<BowlingBall>>any())).thenReturn(true);
        mvc.perform(get("/catalog/search").param("name", "Phaze").param("brand", "Storm").param("weight", "12"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("/catalog?*"))
                .andExpect(header().string("Location", containsString("weight=12")));
        verifyNoInteractions(importer);
    }
    @Test void canonicalRecordIsReusedAndSlugSearchBecomesActualName() throws Exception {
        when(repository.findBySourceKey(CatalogImporter.ballUrl("storm", "phaze-ii"))).thenReturn(Optional.of(ball()));
        mvc.perform(get("/catalog/search").param("name", "phaze-ii").param("brand", "storm"))
                .andExpect(header().string("Location", containsString("name=Phaze+II")))
                .andExpect(header().string("Location", containsString("brand=Storm")));
        verifyNoInteractions(importer);
    }
    @Test void missingBallIsImportedOnceAndFiltersArePreserved() throws Exception {
        String url = CatalogImporter.ballUrl("Storm", "Phaze II");
        when(repository.findBySourceKey(url)).thenReturn(Optional.empty(), Optional.of(ball()));
        when(importer.runBall("Storm", "Phaze II")).thenReturn(new CatalogImporter.ImportReport(1, 0, 0, 0, List.of()));
        mvc.perform(get("/catalog/search").param("name", "Phaze II").param("brand", "Storm")
                .param("coreType", "Symmetric").param("sort", "name-desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("lookupMessage", containsString("Ball added")))
                .andExpect(header().string("Location", containsString("coreType=Symmetric")))
                .andExpect(header().string("Location", containsString("sort=name-desc")));
        verify(importer).runBall("Storm", "Phaze II");
    }
    @Test void blankSearchAndMissingBrandDoNotFetch() throws Exception {
        mvc.perform(get("/catalog/search")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/catalog/search").param("name", "Phaze II"))
                .andExpect(flash().attribute("lookupMessage", containsString("Enter a brand")));
        verifyNoInteractions(importer);
    }
    @Test void networkMalformedAndInvalidInputFailuresShowMessages() throws Exception {
        when(importer.runBall("Storm", "Missing")).thenThrow(new IOException("HTTP 404"));
        mvc.perform(get("/catalog/search").param("name", "Missing").param("brand", "Storm"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("lookupMessage", containsString("Could not retrieve")));
        when(importer.runBall("Storm", "Malformed")).thenReturn(new CatalogImporter.ImportReport(0, 0, 0, 1, List.of("missing title")));
        mvc.perform(get("/catalog/search").param("name", "Malformed").param("brand", "Storm"))
                .andExpect(flash().attribute("lookupMessage", containsString("without usable ball data")));
        mvc.perform(get("/catalog/search").param("name", "../bad").param("brand", "Storm"))
                .andExpect(flash().attribute("lookupMessage", containsString("Use the full Bowwwl")));
        verify(importer, never()).runBall("Storm", "../bad");
    }
    
}
