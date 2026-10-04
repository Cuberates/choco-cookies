package com.stormhacks2026.choco_cookies.tournaments;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Embeddable
public class TournamentEntryFee {
    @Column(name = "event_name", nullable = false, length = 500)
    private String eventName;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 8)
    private String symbol;
    protected TournamentEntryFee() {}
    public TournamentEntryFee(String eventName, BigDecimal amount, String symbol) {
        this.eventName = eventName; this.amount = amount; this.symbol = symbol;
    }
    public String getEventName() { return eventName; }
    public BigDecimal getAmount() { return amount; }
    public String getSymbol() { return symbol; }
}
