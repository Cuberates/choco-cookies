package com.stormhacks2026.choco_cookies.journal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.*;

public record JournalStats(long pins, long games, BigDecimal average, List<Week> weeks) {
    public record Week(LocalDate date, long pins, long games, BigDecimal average, int x, int pinHeight, int averageY) {}
    public static JournalStats from(List<JournalEntry> entries) {
        var totals = new TreeMap<LocalDate, long[]>();
        long pins = 0, games = 0;
        for (var entry : entries) {
            LocalDate week = entry.getBowlingDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            long[] sum = totals.computeIfAbsent(week, key -> new long[2]);
            sum[0] += entry.getPins(); sum[1] += entry.getGames();
            pins += entry.getPins(); games += entry.getGames();
        }
        if (!totals.isEmpty()) {
            LocalDate last = totals.lastKey();
            for (LocalDate date = totals.firstKey(); !date.isAfter(last); date = date.plusWeeks(1)) {
                totals.putIfAbsent(date, new long[2]);
            }
        }
        long maxPins = Math.max(1, totals.values().stream().mapToLong(sum -> sum[0]).max().orElse(1));
        var weeks = new ArrayList<Week>();
        totals.forEach((date, sum) -> {
            BigDecimal avg = average(sum[0], sum[1]);
            weeks.add(new Week(date, sum[0], sum[1], avg, 60 + weeks.size() * 80,
                    (int) Math.round(sum[0] * 170.0 / maxPins),
                    avg == null ? 200 : 200 - (int) Math.round(avg.doubleValue() * 170 / 300)));
        });
        return new JournalStats(pins, games, average(pins, games), List.copyOf(weeks));
    }
    private static BigDecimal average(long pins, long games) {
        return games == 0 ? null : BigDecimal.valueOf(pins).divide(BigDecimal.valueOf(games), 2, RoundingMode.HALF_UP);
    }
    public int chartWidth() { return Math.max(360, weeks.size() * 80 + 60); }
    public long maxWeeklyPins() { return weeks.stream().mapToLong(Week::pins).max().orElse(0); }
}
