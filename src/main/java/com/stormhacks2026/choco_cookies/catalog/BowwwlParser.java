package com.stormhacks2026.choco_cookies.catalog;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.springframework.stereotype.Component;

@Component
public class BowwwlParser {
    public static final String ROOT = "https://www.bowwwl.com/bowling-ball-database";
    private static final Pattern WEIGHT = Pattern.compile("^(\\d{1,2}) pounds?$");
    public record CatalogPage(List<String> ballUrls, String nextUrl) {}

    public static String normalize(String url) {
        URI uri = URI.create(url).normalize();
        if (!"https".equals(uri.getScheme()) || !"www.bowwwl.com".equals(uri.getHost())
                || uri.getPort() != -1 || uri.getUserInfo() != null
                || !uri.getPath().startsWith("/bowling-ball-database")) {
            throw new IllegalArgumentException("Unexpected Bowwwl catalog URL");
        }
        return "https://www.bowwwl.com" + uri.getPath().replaceAll("/+$", "");
    }

    public CatalogPage parseCatalog(String html, String url) {
        Document doc = Jsoup.parse(html, url);
        var cells = doc.select("td.views-field-nothing-2");
        if (cells.isEmpty()) throw new IllegalArgumentException("Catalog table missing; source structure may have changed");
        List<String> urls = new ArrayList<>();
        for (Element cell : cells) {
            Element link = cell.selectFirst("a[href]");
            if (link == null) throw new IllegalArgumentException("Catalog row missing ball link");
            urls.add(normalize(link.absUrl("href")));
        }
        Element next = doc.selectFirst(".pagination a[rel=next]");
        return new CatalogPage(List.copyOf(urls), next == null ? null : next.absUrl("href"));
    }

    public BallData parseBall(String html, String url) {
        Document doc = Jsoup.parse(html, url);
        String sourceUrl = normalize(url);
        Element canonical = doc.selectFirst("link[rel=canonical]");
        if (canonical != null) sourceUrl = normalize(canonical.absUrl("href"));
        String name = text(doc.selectFirst("h1 .field--name-title"));
        if (name == null) throw new IllegalArgumentException("Ball title missing");
        String brand = text(doc.selectFirst(".field--name-field-brand"));
        Element brandImage = doc.selectFirst(".field--name-field-brand img[alt]");
        if (brand == null && brandImage != null) brand = clean(brandImage.attr("alt"));
        Set<Integer> weights = new TreeSet<>();
        for (Element heading : doc.select(".field--name-field-core-specs h6")) {
            var matcher = WEIGHT.matcher(heading.text().trim());
            if (!matcher.matches()) throw new IllegalArgumentException("Unrecognized weight heading");
            int weight = Integer.parseInt(matcher.group(1));
            if (weight < 1 || weight > 16) throw new IllegalArgumentException("Weight outside supported pound range");
            weights.add(weight);
        }
        return new BallData(sourceUrl, sourceUrl, null, name, brand,
            text(doc.selectFirst(".field--name-field-coverstock-type .field__item")),
            text(doc.selectFirst(".field--name-field-core-type .field__item")), Set.copyOf(weights));
    }
    private static String text(Element element) { return element == null ? null : clean(element.text()); }
    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
