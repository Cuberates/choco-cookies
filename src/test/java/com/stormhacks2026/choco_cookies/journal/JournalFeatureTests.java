package com.stormhacks2026.choco_cookies.journal;

import com.stormhacks2026.choco_cookies.auth.*;
import com.stormhacks2026.choco_cookies.catalog.*;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(properties = {"spring.config.import=", "spring.datasource.url=jdbc:h2:mem:journal;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate", "catalog.import=false", "tournaments.import=false"})
@Transactional
class JournalFeatureTests {
    @Autowired WebApplicationContext context;
    @Autowired SessionRepositoryFilter<?> sessionFilter;
    @Autowired JdbcIndexedSessionRepository sessions;
    @Autowired JdbcTemplate jdbc;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JournalService journal;
    @Autowired JournalEntryRepository entries;
    @Autowired BowlingBallRepository balls;
    MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(sessionFilter).apply(springSecurity()).build();
        users.saveAndFlush(new AppUser("alice", encoder.encode("test-password")));
        users.saveAndFlush(new AppUser("bob", encoder.encode("other-password")));
    }
    JournalForm form(LocalDate date, int games, int pins) {
        var form = new JournalForm(); form.setBowlingDate(date); form.setLeagueName("Thursday league");
        form.setAlleyName("Town Lanes"); form.setLocation("Vancouver, BC"); form.setGames(games); form.setPins(pins);
        return form;
    }
    MockHttpServletRequestBuilder entryPost(String path, LocalDate date, int games, int pins) {
        return post(path).with(user("alice")).with(csrf()).param("bowlingDate", date.toString())
                .param("leagueName", "Thursday league").param("alleyName", "Town Lanes")
                .param("location", "Vancouver, BC").param("games", Integer.toString(games)).param("pins", Integer.toString(pins));
    }
    @Test void registrationHashesPasswordsRejectsDuplicatesAndNeverEchoesPasswords() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("username", "New_User").param("password", "new-password"))
                .andExpect(redirectedUrl("/login?registered"));
        var account = users.findByUsername("new_user").orElseThrow();
        assertNotEquals("new-password", account.getPasswordHash());
        assertTrue(encoder.matches("new-password", account.getPasswordHash()));
        mvc.perform(post("/register").with(csrf()).param("username", "NEW_USER").param("password", "new-password"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("already taken")))
                .andExpect(content().string(not(containsString("value=\"new-password\""))));
        mvc.perform(post("/register").with(csrf()).param("username", "short").param("password", "tiny"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("registration", "password"));
        mvc.perform(post("/register").with(csrf()).param("username", "unicode").param("password", "😀".repeat(20)))
                .andExpect(model().attributeHasFieldErrors("registration", "passwordWithinByteLimit"));
        assertTrue(users.findByUsername("short").isEmpty());
        assertTrue(users.findByUsername("unicode").isEmpty());
        mvc.perform(post("/register").param("username", "csrfuser").param("password", "new-password"))
                .andExpect(status().isForbidden());
    }
    @Test void actualLoginPersistsJdbcSessionAndLogoutInvalidatesIt() throws Exception {
        var loginPage = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn().getResponse();
        Cookie preLoginCookie = loginPage.getCookie("SESSION");
        assertNotNull(preLoginCookie);
        String token = Objects.requireNonNull(Jsoup.parse(loginPage.getContentAsString()).selectFirst("input[name=_csrf]")).val();
        var login = mvc.perform(post("/login").cookie(preLoginCookie).param("_csrf", token)
                .param("username", "ALICE").param("password", "test-password"))
                .andExpect(redirectedUrl("/journal")).andReturn().getResponse();
        Cookie cookie = login.getCookie("SESSION"); assertNotNull(cookie);
        assertTrue(login.getHeaders("Set-Cookie").stream().anyMatch(value -> value.contains("HttpOnly") && value.contains("SameSite=Lax")));
        String sessionId = new String(Base64.getDecoder().decode(cookie.getValue()), StandardCharsets.UTF_8);
        String oldId = new String(Base64.getDecoder().decode(preLoginCookie.getValue()), StandardCharsets.UTF_8);
        assertNotEquals(oldId, sessionId);
        assertNull(sessions.findById(oldId));
        org.springframework.session.Session session = sessions.findById(sessionId); assertNotNull(session);
        assertEquals(1800, session.getMaxInactiveInterval().toSeconds());
        assertNotNull(session.getAttribute("SPRING_SECURITY_CONTEXT"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION WHERE SESSION_ID = ? AND PRINCIPAL_NAME = 'alice'", Integer.class, sessionId));
        var page = mvc.perform(get("/journal").cookie(cookie)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Your first session starts here"))).andReturn().getResponse();
        String logoutToken = Objects.requireNonNull(Jsoup.parse(page.getContentAsString()).selectFirst("form[action='/logout'] input[name=_csrf]")).val();
        mvc.perform(post("/logout").cookie(cookie).param("_csrf", logoutToken)).andExpect(redirectedUrl("/login?logout"));
        assertNull(sessions.findById(sessionId));
        mvc.perform(get("/journal").cookie(cookie)).andExpect(status().is3xxRedirection());
        mvc.perform(post("/login").with(csrf()).param("username", "alice").param("password", "wrong"))
                .andExpect(redirectedUrl("/login?error"));
    }
    @Test void privateRoutesRequireAuthenticationAndWritesRequireCsrf() throws Exception {
        mvc.perform(get("/journal")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/journal/new")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/journal").with(user("alice"))).andExpect(status().isForbidden());
        mvc.perform(get("/catalog")).andExpect(status().isOk()).andExpect(content().string(containsString("href=\"/login\"")));
        mvc.perform(get("/tournaments")).andExpect(status().isOk());
        mvc.perform(get("/locations")).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
    }
    @Test void journalCrudLinksCatalogBallsEscapesNotesAndUpdatesStats() throws Exception {
        var ball = new BowlingBall();
        ball.update(new BallData("test-phaze", "https://example.com/phaze", null, "Phaze II", "Storm", "Solid", "Symmetric", Set.of(15)));
        balls.saveAndFlush(ball);
        mvc.perform(get("/journal/new").with(user("alice"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Phaze II"))).andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(entryPost("/journal", LocalDate.now(), 3, 600).param("ballIds", ball.getId().toString()).param("notes", "<script>unsafe</script>"))
                .andExpect(redirectedUrl("/journal?saved"));
        var saved = journal.list("alice").get(0);
        assertEquals(1, saved.getBalls().size()); assertEquals(new BigDecimal("200.00"), saved.getAverage());
        mvc.perform(get("/journal").with(user("alice"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Weekly performance")))
                .andExpect(content().string(containsString("Phaze II")))
                .andExpect(content().string(containsString("&lt;script&gt;unsafe&lt;/script&gt;")))
                .andExpect(content().string(containsString("200.00")));
        mvc.perform(get("/journal/" + saved.getId() + "/edit").with(user("alice"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("checked=\"checked\"")));
        mvc.perform(entryPost("/journal/" + saved.getId(), LocalDate.now(), 2, 500)).andExpect(redirectedUrl("/journal?saved"));
        assertEquals(new BigDecimal("250.00"), JournalStats.from(journal.list("alice")).average());
        assertTrue(journal.list("alice").get(0).getBalls().isEmpty());
        mvc.perform(post("/journal/" + saved.getId() + "/delete").with(user("alice")).with(csrf()))
                .andExpect(redirectedUrl("/journal?deleted"));
        assertTrue(journal.list("alice").isEmpty());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM journal_entry_ball", Integer.class));
    }
    @Test void ownershipChecksBlockReadsUpdatesAndDeletes() throws Exception {
        journal.save(null, "bob", form(LocalDate.now(), 3, 450));
        Long id = journal.list("bob").get(0).getId();
        mvc.perform(get("/journal").with(user("alice"))).andExpect(content().string(containsString("Your first session starts here")));
        mvc.perform(get("/journal/" + id + "/edit").with(user("alice"))).andExpect(status().isNotFound());
        mvc.perform(entryPost("/journal/" + id, LocalDate.now(), 1, 300)).andExpect(status().isNotFound());
        mvc.perform(post("/journal/" + id + "/delete").with(user("alice")).with(csrf())).andExpect(status().isNotFound());
        assertEquals(450, journal.list("bob").get(0).getPins());
    }
    @Test void validationRejectsImpossibleScoresFutureDatesAndInvalidBallSelections() throws Exception {
        mvc.perform(entryPost("/journal", LocalDate.now(), 1, 301)).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("entry", "scoreValid"));
        mvc.perform(entryPost("/journal", LocalDate.now().plusDays(1), 1, 200))
                .andExpect(model().attributeHasFieldErrors("entry", "bowlingDate"));
        mvc.perform(entryPost("/journal", LocalDate.now(), 0, 0)).andExpect(model().attributeHasFieldErrors("entry", "games"));
        mvc.perform(entryPost("/journal", LocalDate.now(), 1, -1)).andExpect(model().attributeHasFieldErrors("entry", "pins"));
        mvc.perform(entryPost("/journal", LocalDate.now(), 1, 200).with(request -> { request.setParameter("leagueName", " "); return request; }))
                .andExpect(model().attributeHasFieldErrors("entry", "leagueName"));
        mvc.perform(entryPost("/journal", LocalDate.now(), 1, 200).param("ballIds", "987654321"))
                .andExpect(model().attributeHasFieldErrors("entry", "ballIds"));
        mvc.perform(entryPost("/journal", LocalDate.now(), 1, 200).param("ballIds", "1", "1"))
                .andExpect(model().attributeHasFieldErrors("entry", "ballIds"));
        mvc.perform(entryPost("/journal", LocalDate.now(), 1, 200).param("ballIds", "1", "2", "3", "4", "5", "6", "7"))
                .andExpect(model().attributeHasFieldErrors("entry", "ballIds"));
        assertTrue(journal.list("alice").isEmpty());
    }
    @Test void weeklyAggregationWeightsGamesAndHandlesIsoYearBoundaryAndEmptyWeeks() {
        journal.save(null, "alice", form(LocalDate.of(2025, 12, 29), 1, 300));
        journal.save(null, "alice", form(LocalDate.of(2026, 1, 1), 3, 300));
        journal.save(null, "alice", form(LocalDate.of(2026, 1, 12), 2, 400));
        var stats = JournalStats.from(journal.list("alice"));
        assertEquals(1000, stats.pins()); assertEquals(6, stats.games());
        assertEquals(new BigDecimal("166.67"), stats.average());
        assertEquals(3, stats.weeks().size());
        assertEquals(LocalDate.of(2025, 12, 29), stats.weeks().get(0).date());
        assertEquals(new BigDecimal("150.00"), stats.weeks().get(0).average());
        assertEquals(0, stats.weeks().get(1).games()); assertNull(stats.weeks().get(1).average());
        assertNull(JournalStats.from(List.of()).average());
    }
}
