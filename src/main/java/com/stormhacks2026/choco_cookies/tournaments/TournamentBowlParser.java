package com.stormhacks2026.choco_cookies.tournaments;

import java.math.BigDecimal;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.springframework.stereotype.Component;

@Component
public class TournamentBowlParser {
    public static final String ROOT = "https://tournamentbowl.com";
    public static final String UPCOMING = ROOT + "/open/tournaments.cfm?which=upcoming";
    private static final Pattern DATE = Pattern.compile("(?i)\\b(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+(\\d{1,2})\\s*,\\s*(\\d{4})\\b");
    private static final Pattern FEE = Pattern.compile("(?i)^(.+?)\\s*-\\s*([$€£])\\s*(\\d[\\d,]*(?:\\.\\d{1,2})?)\\s+entry\\b.*$");
    public record Listing(Long sourceId, String sourceUrl, String title, String venueName, String city, String region) {}

    public static String detailUrl(long id) {
        if (id < 1) throw new IllegalArgumentException("Tournament ID must be positive");
        return ROOT + "/open/TournamentHome.cfm?ID_Tournament=" + id;
    }
    public static long sourceId(String url) {
        URI uri = URI.create(url).normalize();
        if (!"https".equals(uri.getScheme()) || !"tournamentbowl.com".equals(uri.getHost())
                || uri.getPort() != -1 || uri.getUserInfo() != null
                || !"/open/tournamenthome.cfm".equalsIgnoreCase(uri.getPath()) || uri.getRawQuery() == null)
            throw new IllegalArgumentException("Unexpected Tournament Bowl detail URL");
        Long id = null;
        for (String part : uri.getRawQuery().split("&")) {
            String[] pair = part.split("=", 2);
            if (URLDecoder.decode(pair[0], StandardCharsets.UTF_8).equalsIgnoreCase("id_tournament")) {
                if (id != null || pair.length != 2 || !pair[1].matches("[0-9]{1,18}"))
                    throw new IllegalArgumentException("Invalid Tournament Bowl ID");
                id = Long.parseLong(pair[1]);
            }
        }
        if (id == null || id < 1) throw new IllegalArgumentException("Missing Tournament Bowl ID");
        return id;
    }
    public List<Listing> parseListing(String html) {
        Document doc = Jsoup.parse(html, UPCOMING);
        Element table = doc.selectFirst("table.card");
        if (table == null) throw new IllegalArgumentException("Tournament list missing; source structure may have changed");
        var listings = new ArrayList<Listing>();
        for (Element row : table.select("tr")) {
            Element link = row.select("a[href]").stream()
                    .filter(a -> a.attr("href").toLowerCase(Locale.ROOT).contains("tournamenthome.cfm"))
                    .findFirst().orElse(null);
            if (link == null) continue;
            long id;
            try { id = sourceId(link.absUrl("href")); }
            catch (IllegalArgumentException ex) { continue; }
            var cells = row.children().stream().filter(el -> el.tagName().equals("td")).toList();
            List<String> location = cells.isEmpty() ? List.of() : lines(cells.get(cells.size() - 1));
            String venue = location.isEmpty() ? null : location.get(0);
            String[] cityRegion = splitLocation(location.size() > 1 ? location.get(location.size() - 1) : null);
            listings.add(new Listing(id, detailUrl(id), clean(link.text()), venue, cityRegion[0], cityRegion[1]));
        }
        if (listings.isEmpty() && !table.text().toLowerCase(Locale.ROOT).matches("(?s).*(no tournaments|no upcoming|no results).*"))
            throw new IllegalArgumentException("No recognizable tournament links in source table");
        return List.copyOf(listings);
    }
    public TournamentData parseDetail(String html, Listing listing) {
        Document doc = Jsoup.parse(html, listing.sourceUrl());
        Element card = doc.select(".card").stream().filter(el -> el.selectFirst("h2") != null
                && el.selectFirst("h2").text().toLowerCase(Locale.ROOT).startsWith("welcome")).findFirst().orElse(null);
        if (card == null) throw new IllegalArgumentException("Tournament welcome card missing");
        String title = clean(doc.title());
        if (title == null) title = clean(card.selectFirst("h2").text().replaceFirst("(?i)^Welcome to the\\s+", ""));
        if (title == null || title.length() > 1000) throw new IllegalArgumentException("Tournament title missing or too long");
        var copy = card.clone(); copy.select("h2").remove();
        var lines = lines(copy);
        String venue = listing.venueName(), address = null, city = listing.city(), region = listing.region(), dateText = null;
        LocalDate start = null, end = null;
        int hosted = -1, venueLine = -1, dateLine = -1, feeLine = lines.size();
        var fees = new ArrayList<TournamentEntryFee>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("Hosted by")) {
                hosted = i;
                String value = clean(line.substring("Hosted by".length()));
                venueLine = value == null ? i + 1 : i;
                if (value != null) venue = value;
                else if (venueLine < lines.size() && !DATE.matcher(lines.get(venueLine)).find()
                        && !FEE.matcher(lines.get(venueLine)).matches()) venue = lines.get(venueLine);
            }
            var date = DATE.matcher(line);
            if (dateText == null && date.find()) {
                dateText = line; dateLine = i;
                try {
                    start = parseDate(date.group(1), date.group(2), date.group(3)); end = start;
                    if (date.find()) end = parseDate(date.group(1), date.group(2), date.group(3));
                    if (end.isBefore(start)) { start = null; end = null; }
                } catch (DateTimeException ex) { start = null; end = null; }
            }
            var fee = FEE.matcher(line);
            if (fee.matches()) {
                feeLine = Math.min(feeLine, i);
                BigDecimal amount = new BigDecimal(fee.group(3).replace(",", ""));
                if (amount.precision() - amount.scale() > 8 || fee.group(1).length() > 500)
                    throw new IllegalArgumentException("Entry fee outside supported storage range");
                fees.add(new TournamentEntryFee(fee.group(1).trim(), amount, fee.group(2)));
            }
        }
        if (hosted >= 0 && venueLine >= 0) {
            int stop = dateLine >= 0 ? dateLine : feeLine;
            var addressLines = new ArrayList<String>();
            for (int i = venueLine + 1; i < stop; i++) {
                String line = lines.get(i);
                if (line.startsWith("Ask your") || line.equals("Contact Director")) break;
                addressLines.add(line);
            }
            if (!addressLines.isEmpty()) {
                address = String.join("\n", addressLines);
                String[] location = splitLocation(addressLines.get(addressLines.size() - 1));
                if (location[0] != null && location[1] != null) { city = location[0]; region = location[1]; }
            }
        }
        return new TournamentData(listing.sourceId(), detailUrl(listing.sourceId()), title,
                limited(venue, 255), limited(city, 255), limited(region, 64), limited(address, 1000),
                start, end, limited(dateText, 255), fees);
    }
    private static LocalDate parseDate(String month, String day, String year) {
        int number = List.of("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
                .indexOf(month.substring(0, 3).toLowerCase(Locale.ROOT)) + 1;
        return LocalDate.of(Integer.parseInt(year), number, Integer.parseInt(day));
    }
    private static String[] splitLocation(String value) {
        if (value == null || !value.contains(",")) return new String[] {null, null};
        int comma = value.lastIndexOf(',');
        return new String[] {clean(value.substring(0, comma)), clean(value.substring(comma + 1).replaceFirst("\\s+\\d[\\w -]*$", ""))};
    }
    private static List<String> lines(Element element) {
        var copy = element.clone(); copy.select("script, style").remove();
        // Source-code newlines are whitespace, whereas <br> separates visible fields.
        for (Element child : copy.getAllElements()) {
            for (Node node : child.childNodes()) {
                if (node instanceof TextNode text) text.text(text.getWholeText().replaceAll("\\s+", " "));
            }
        }
        copy.select("br").forEach(br -> br.after(new TextNode("\n")));
        return Arrays.stream(copy.wholeText().split("\\R")).map(TournamentBowlParser::clean)
                .filter(Objects::nonNull).toList();
    }
    private static String clean(String value) {
        if (value == null) return null;
        String clean = value.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
        return clean.isEmpty() ? null : clean;
    }
    private static String limited(String value, int length) {
        return value == null || value.length() <= length ? value : value.substring(0, length);
    }
}
