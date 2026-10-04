package com.stormhacks2026.choco_cookies.journal;

import com.stormhacks2026.choco_cookies.auth.AppUser;
import com.stormhacks2026.choco_cookies.catalog.BowlingBall;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "journal_entry")
public class JournalEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
    @Column(nullable = false) private LocalDate bowlingDate;
    @Column(nullable = false, length = 160) private String leagueName;
    @Column(nullable = false, length = 160) private String alleyName;
    @Column(nullable = false, length = 160) private String location;
    @Column(nullable = false) private int games;
    @Column(nullable = false) private int pins;
    @Column(nullable = false, length = 2000) private String notes = "";
    @ManyToMany
    @JoinTable(name = "journal_entry_ball", joinColumns = @JoinColumn(name = "entry_id"), inverseJoinColumns = @JoinColumn(name = "ball_id"))
    private Set<BowlingBall> balls = new LinkedHashSet<>();

    protected JournalEntry() {}
    public JournalEntry(AppUser user) { this.user = user; }
    public void update(JournalForm form, Set<BowlingBall> balls) {
        bowlingDate = form.getBowlingDate(); leagueName = form.getLeagueName(); alleyName = form.getAlleyName();
        location = form.getLocation(); games = form.getGames(); pins = form.getPins(); notes = form.getNotes();
        this.balls.clear(); this.balls.addAll(balls);
    }
    public Long getId() { return id; }
    public LocalDate getBowlingDate() { return bowlingDate; }
    public String getLeagueName() { return leagueName; }
    public String getAlleyName() { return alleyName; }
    public String getLocation() { return location; }
    public int getGames() { return games; }
    public int getPins() { return pins; }
    public String getNotes() { return notes; }
    public Set<BowlingBall> getBalls() { return Set.copyOf(balls); }
    public BigDecimal getAverage() { return BigDecimal.valueOf(pins).divide(BigDecimal.valueOf(games), 2, RoundingMode.HALF_UP); }
}
