package com.stormhacks2026.choco_cookies.tournaments;

import java.time.LocalDate;
import java.util.List;

public record TournamentData(Long sourceId, String sourceUrl, String title, String venueName,
        String city, String region, String address, LocalDate startDate, LocalDate endDate,
        String dateText, List<TournamentEntryFee> entryFees) {
    public TournamentData { entryFees = List.copyOf(entryFees); }
}
