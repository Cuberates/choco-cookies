package com.stormhacks2026.choco_cookies.catalog;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class BowwwlFetcher {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    private final List<Pattern> denied = new ArrayList<>();
    private long lastRequest;
    private boolean rulesLoaded;

    public synchronized String fetch(String url) throws IOException, InterruptedException {
        BowwwlParser.normalize(url);
        if (!rulesLoaded) {
            String rules = request("https://www.bowwwl.com/robots.txt");
            // Conservatively honor every Disallow group, including agent-specific restrictions.
            for (String line : rules.split("\\R")) {
                String directive = line.split("#", 2)[0].trim();
                if (directive.toLowerCase(Locale.ROOT).startsWith("disallow:")) {
                    String path = directive.substring(9).trim();
                    if (!path.isEmpty()) {
                        boolean anchored = path.endsWith("$");
                        if (anchored) path = path.substring(0, path.length() - 1);
                        String regex = Arrays.stream(path.split("\\*", -1)).map(Pattern::quote)
                            .collect(java.util.stream.Collectors.joining(".*"));
                        denied.add(Pattern.compile("^" + regex + (anchored ? "$" : ".*")));
                    }
                }
            }
            rulesLoaded = true;
        }

        URI uri = URI.create(url);
        String path = uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());

        System.out.println(uri);
        if (denied.stream().anyMatch(rule -> rule.matcher(path).matches()))
            throw new IOException("Bowwwl robots rules prohibit the requested path");
        return request(url);
    }

    private String request(String url) throws IOException, InterruptedException {
        long wait = 2000 - (System.currentTimeMillis() - lastRequest);
        if (wait > 0) Thread.sleep(wait);
        lastRequest = System.currentTimeMillis();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
            .header("User-Agent", "ChocoCookiesCatalogImporter/1.0").GET().build();
        // Redirects are deliberately not followed, and failures abort rather than retry aggressively.
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Bowwwl returned HTTP " + response.statusCode());
        if (!response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT).contains("text/"))
            throw new IOException("Unexpected Bowwwl content type");
        return response.body();
    }
}
