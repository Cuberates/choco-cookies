package com.stormhacks2026.choco_cookies.tournaments;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@Transactional(readOnly = true)
public class TournamentController {
    private final TournamentRepository repository;
    public TournamentController(TournamentRepository repository) { this.repository = repository; }

    @GetMapping("/tournaments")
    public String tournaments(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String venue, @RequestParam(defaultValue = "") String region,
            @RequestParam(required = false) String city, @RequestParam(defaultValue = "false") boolean venueMissing,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "date") String sort, @RequestParam(defaultValue = "0") int page, Model model) {
        if (from != null && to != null && from.isAfter(to))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must be before end date");
        q = q.trim(); venue = venue.trim(); region = region.trim();
        sort = "date-desc".equals(sort) ? "date-desc" : "date";
        var filter = filters(q, venue, region, city, venueMissing, from, to, LocalDate.now());
        final boolean descending = "date-desc".equals(sort);
        Specification<Tournament> ordered = (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class) {
                var missing = cb.<Integer>selectCase().when(cb.isNull(root.get("startDate")), 1).otherwise(0);
                query.orderBy(cb.asc(missing), descending ? cb.desc(root.get("startDate")) : cb.asc(root.get("startDate")), cb.asc(root.get("id")));
            }
            return filter.toPredicate(root, query, cb);
        };
        long count = repository.count(filter);
        int lastPage = (int) Math.min(Integer.MAX_VALUE, Math.max(0, (count - 1) / 20));
        var results = repository.findAll(ordered, PageRequest.of(Math.min(Math.max(0, page), lastPage), 20));
        model.addAttribute("tournaments", results.getContent().stream().map(TournamentView::of).toList());
        model.addAttribute("results", results);
        model.addAttribute("q", q); model.addAttribute("venue", venue); model.addAttribute("region", region);
        model.addAttribute("city", city); model.addAttribute("venueMissing", venueMissing);
        model.addAttribute("from", from); model.addAttribute("to", to); model.addAttribute("sort", sort);
        common(model);
        return "tournaments";
    }

    @GetMapping("/locations")
    public String locations(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String region, @RequestParam(defaultValue = "0") int page, Model model) {
        q = q.trim(); region = region.trim();
        var groups = new LinkedHashMap<LocationKey, List<Tournament>>();
        for (var tournament : repository.findAll(filters(q, "", region, null, false, null, null, LocalDate.now()))) {
            var key = new LocationKey(key(tournament.getVenueName()), key(tournament.getCity()), key(tournament.getRegion()));
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(tournament);
        }
        var locations = groups.values().stream().map(events -> {
            var first = events.get(0);
            var nextDate = events.stream().map(Tournament::getStartDate).filter(Objects::nonNull).min(LocalDate::compareTo).orElse(null);
            return new LocationView(first.getVenueName(), first.getCity(), first.getRegion(), events.size(), nextDate);
        }).sorted(Comparator.comparing((LocationView v) -> display(v.venueName()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(v -> display(v.city())).thenComparing(v -> display(v.region()))).toList();
        int last = Math.max(0, (locations.size() - 1) / 20), current = Math.min(Math.max(0, page), last);
        int start = current * 20;
        var visible = locations.subList(start, Math.min(start + 20, locations.size()));
        model.addAttribute("locations", visible);
        model.addAttribute("results", new PageImpl<>(visible, PageRequest.of(current, 20), locations.size()));
        model.addAttribute("q", q); model.addAttribute("region", region);
        common(model);
        return "locations";
    }
    private void common(Model model) {
        var upcoming = repository.findAll(filters("", "", "", null, false, null, null, LocalDate.now()));
        model.addAttribute("upcomingCount", upcoming.size());
        model.addAttribute("venues", upcoming.stream().map(Tournament::getVenueName).filter(Objects::nonNull).distinct().sorted().toList());
        model.addAttribute("regions", upcoming.stream().map(Tournament::getRegion).filter(Objects::nonNull).distinct().sorted().toList());
    }
    static Specification<Tournament> filters(String q, String venue, String region, String city, boolean venueMissing,
            LocalDate from, LocalDate to, LocalDate today) {
        return (root, query, cb) -> {
            var conditions = new ArrayList<Predicate>();
            var finish = cb.<LocalDate>coalesce(root.get("endDate"), root.get("startDate"));
            conditions.add(cb.or(cb.isNull(finish), cb.greaterThanOrEqualTo(finish, today)));
            if (!q.isBlank()) {
                String escaped = q.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                var fields = List.of("title", "venueName", "city", "region");
                conditions.add(cb.or(fields.stream().map(field -> cb.like(cb.lower(root.get(field)), "%" + escaped + "%", '\\')).toArray(Predicate[]::new)));
            }
            if (!venue.isBlank()) conditions.add(cb.equal(cb.lower(root.get("venueName")), venue.toLowerCase(Locale.ROOT)));
            if (venueMissing) conditions.add(cb.isNull(root.get("venueName")));
            if (!region.isBlank()) conditions.add(cb.equal(cb.lower(root.get("region")), region.toLowerCase(Locale.ROOT)));
            if (city != null) conditions.add(city.isBlank() ? cb.isNull(root.get("city")) : cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase(Locale.ROOT)));
            if (from != null) conditions.add(cb.greaterThanOrEqualTo(finish, from));
            if (to != null) conditions.add(cb.lessThanOrEqualTo(root.get("startDate"), to));
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }
    private static String key(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private static String display(String value) { return value == null || value.isBlank() ? "Unspecified" : value; }
    private record LocationKey(String venue, String city, String region) {}
    public record LocationView(String venueName, String city, String region, int count, LocalDate nextDate) {
        public String getVenueLabel() { return venueName == null ? "Venue not listed" : venueName; }
        public String getLocationLabel() { return location(city, region); }
        public String getNextDateLabel() { return nextDate == null ? "Date not listed" : date(nextDate); }
    }
    public record FeeView(String eventName, String price) {}
    public record TournamentView(String title, String venueName, String locationLabel, String address,
            String dateLabel, String priceLabel, String sourceUrl, List<FeeView> fees) {
        static TournamentView of(Tournament tournament) {
            String dates = tournament.getStartDate() == null ? displayDate(tournament.getDateText()) : date(tournament.getStartDate());
            if (tournament.getStartDate() != null && tournament.getEndDate() != null && !tournament.getStartDate().equals(tournament.getEndDate()))
                dates += " – " + date(tournament.getEndDate());
            return new TournamentView(tournament.getTitle(), tournament.getVenueName() == null ? "Venue not listed" : tournament.getVenueName(),
                    location(tournament.getCity(), tournament.getRegion()), tournament.getAddress(), dates,
                    tournament.getEntryPriceFrom() == null ? "Entry price not listed" : money(tournament.getCurrencySymbol(), tournament.getEntryPriceFrom()) + " starting at",
                    tournament.getSourceUrl(), tournament.getEntryFees().stream().map(fee -> new FeeView(fee.getEventName(), money(fee.getSymbol(), fee.getAmount()))).toList());
        }
    }
    private static String displayDate(String value) { return value == null ? "Date not listed" : value; }
    private static String location(String city, String region) {
        return city == null && region == null ? "Location not listed" : String.join(", ", java.util.stream.Stream.of(city, region).filter(Objects::nonNull).toList());
    }
    private static String date(LocalDate date) { return date.format(DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH)); }
    private static String money(String symbol, BigDecimal amount) { return symbol + amount.stripTrailingZeros().toPlainString(); }
}
