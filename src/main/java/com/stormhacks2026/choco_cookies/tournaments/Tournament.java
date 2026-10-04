package com.stormhacks2026.choco_cookies.tournaments;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "tournament")
public class Tournament {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Long sourceId;
    @Column(nullable = false, length = 1024)
    private String sourceUrl;
    @Column(nullable = false, length = 1000)
    private String title;
    private String venueName;
    private String city;
    @Column(length = 64)
    private String region;
    @Column(length = 1000)
    private String address;
    private LocalDate startDate;
    private LocalDate endDate;
    private String dateText;
    @Column(precision = 10, scale = 2)
    private BigDecimal entryPriceFrom;
    @Column(length = 8)
    private String currencySymbol;
    @Column(nullable = false)
    private Instant importedAt;
    @ElementCollection
    @CollectionTable(name = "tournament_entry_fee", joinColumns = @JoinColumn(name = "tournament_id"))
    @OrderColumn(name = "position")
    private List<TournamentEntryFee> entryFees = new ArrayList<>();

    public Tournament() {}
    public void update(TournamentData data) {
        sourceId = data.sourceId(); sourceUrl = data.sourceUrl(); title = data.title();
        venueName = data.venueName(); city = data.city(); region = data.region(); address = data.address();
        startDate = data.startDate(); endDate = data.endDate(); dateText = data.dateText();
        entryFees.clear(); entryFees.addAll(data.entryFees());
        // Free advancement rounds should not make a paid tournament look free.
        var positive = entryFees.stream().filter(fee -> fee.getAmount().signum() > 0).toList();
        var lowest = (positive.isEmpty() ? entryFees : positive).stream()
                .min(Comparator.comparing(TournamentEntryFee::getAmount));
        // Different printed currencies cannot be compared numerically.
        if (entryFees.stream().map(TournamentEntryFee::getSymbol).distinct().count() > 1) lowest = Optional.empty();
        entryPriceFrom = lowest.map(TournamentEntryFee::getAmount).orElse(null);
        currencySymbol = lowest.map(TournamentEntryFee::getSymbol).orElse(null);
        importedAt = Instant.now();
    }
    public Long getId() { return id; }
    public Long getSourceId() { return sourceId; }
    public String getSourceUrl() { return sourceUrl; }
    public String getTitle() { return title; }
    public String getVenueName() { return venueName; }
    public String getCity() { return city; }
    public String getRegion() { return region; }
    public String getAddress() { return address; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getDateText() { return dateText; }
    public BigDecimal getEntryPriceFrom() { return entryPriceFrom; }
    public String getCurrencySymbol() { return currencySymbol; }
    public Instant getImportedAt() { return importedAt; }
    public List<TournamentEntryFee> getEntryFees() { return List.copyOf(entryFees); }
}
