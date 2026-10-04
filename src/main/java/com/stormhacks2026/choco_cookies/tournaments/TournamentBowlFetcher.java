package com.stormhacks2026.choco_cookies.tournaments;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TournamentBowlFetcher {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    private final List<Pattern> denied = new ArrayList<>();
    private boolean robotsLoaded;
    private long lastRequest;
    private long delayMillis = 10_000;

    public synchronized String fetch(String url) throws IOException, InterruptedException {
        if (!TournamentBowlParser.UPCOMING.equals(url)) TournamentBowlParser.sourceId(url);
        if (!robotsLoaded) {
            var response = request(TournamentBowlParser.ROOT + "/robots.txt");
            if (response.statusCode() == 200) {
                for (String line : response.body().split("\\R")) {
                    String directive = line.split("#", 2)[0].trim();
                    int colon = directive.indexOf(':');
                    if (colon < 0) continue;
                    String key = directive.substring(0, colon).toLowerCase(Locale.ROOT);
                    String value = directive.substring(colon + 1).trim();
                    if (key.equals("crawl-delay")) {
                        try {
                            double seconds = Double.parseDouble(value);
                            if (!Double.isFinite(seconds) || seconds < 0) throw new NumberFormatException("Invalid delay");
                            delayMillis = Math.max(delayMillis, (long) Math.ceil(seconds * 1000));
                        }
                        catch (NumberFormatException ex) { throw new IOException("Malformed Tournament Bowl crawl delay", ex); }
                    }
                    if (key.equals("disallow") && !value.isEmpty()) {
                        boolean anchored = value.endsWith("$");
                        if (anchored) value = value.substring(0, value.length() - 1);
                        String regex = Arrays.stream(value.split("\\*", -1)).map(Pattern::quote)
                                .collect(java.util.stream.Collectors.joining(".*"));
                        denied.add(Pattern.compile("^" + regex + (anchored ? "$" : ".*")));
                    }
                }
            } else if (response.statusCode() != 404) {
                throw new IOException("Could not verify Tournament Bowl robots rules: HTTP " + response.statusCode());
            }
            robotsLoaded = true;
        }
        URI uri = URI.create(url);
        String path = uri.getRawPath() + "?" + uri.getRawQuery();
        if (denied.stream().anyMatch(rule -> rule.matcher(path).matches()))
            throw new IOException("Tournament Bowl robots rules prohibit this path");
        var response = request(url);
        if (response.statusCode() != 200) throw new IOException("Tournament Bowl returned HTTP " + response.statusCode());
        if (!response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT).contains("text/html"))
            throw new IOException("Unexpected Tournament Bowl content type");
        return response.body();
    }
    private HttpResponse<String> request(String url) throws IOException, InterruptedException {
        long wait = delayMillis - (System.currentTimeMillis() - lastRequest);
        if (wait > 0) Thread.sleep(wait);
        lastRequest = System.currentTimeMillis();
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("User-Agent", "BowlerJournalTournamentImporter/1.0").GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
