package com.stormhacks2026.choco_cookies.journal;

import com.stormhacks2026.choco_cookies.auth.AppUser;
import com.stormhacks2026.choco_cookies.auth.AppUserRepository;
import com.stormhacks2026.choco_cookies.catalog.BowlingBall;
import com.stormhacks2026.choco_cookies.catalog.BowlingBallRepository;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class JournalService {
    private final JournalEntryRepository entries;
    private final AppUserRepository users;
    private final BowlingBallRepository balls;
    public JournalService(JournalEntryRepository entries, AppUserRepository users, BowlingBallRepository balls) {
        this.entries = entries; this.users = users; this.balls = balls;
    }
    private AppUser user(String username) {
        return users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    public List<JournalEntry> list(String username) { return entries.findByUserIdOrderByBowlingDateDescIdDesc(user(username).getId()); }
    public JournalEntry owned(Long id, String username) {
        return entries.findByIdAndUserId(id, user(username).getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    public Set<BowlingBall> selectedBalls(List<Long> ids) {
        if (ids.size() > 6 || ids.contains(null) || new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("Choose up to six distinct catalog balls.");
        }
        var selected = new LinkedHashSet<>(balls.findAllById(ids));
        if (selected.size() != ids.size()) throw new IllegalArgumentException("One of the selected balls is no longer in the catalog.");
        return selected;
    }
    @Transactional public void save(Long id, String username, JournalForm form) {
        var entry = id == null ? new JournalEntry(user(username)) : owned(id, username);
        entry.update(form, selectedBalls(form.getBallIds()));
        entries.save(entry);
    }
    @Transactional public void delete(Long id, String username) { entries.delete(owned(id, username)); }
}
