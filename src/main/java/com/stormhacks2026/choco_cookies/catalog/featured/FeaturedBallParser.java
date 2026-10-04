package com.stormhacks2026.choco_cookies.catalog.featured;

import com.stormhacks2026.choco_cookies.catalog.BowwwlParser;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class FeaturedBallParser {
    public record Detail(String name, String sourceUrl, String ballImageUrl, String coreImageUrl,
                         String coreName, String coverstockType, String coreType) {}

    public List<String> candidates(String html, String brandSlug) {
        String base = brandUrl(brandSlug);
        var doc = Jsoup.parse(html, base);
        var urls = new LinkedHashSet<String>();
        for (Element link : doc.select("a.card-link[rel=bookmark][href], td.views-field-nothing-2 a[href]")) {
            try {
                String url = BowwwlParser.normalize(link.absUrl("href"));
                if (isBallUrl(url, brandSlug)) urls.add(url);
            } catch (IllegalArgumentException ignored) { /* Ignore unrelated links. */ }
        }
        if (urls.isEmpty()) throw new IllegalArgumentException("No ball detail links in brand listing");
        return List.copyOf(urls);
    }

    public Detail detail(String html, String url, String brandSlug) {
        String normalized = BowwwlParser.normalize(url);
        if (!isBallUrl(normalized, brandSlug)) throw new IllegalArgumentException("Ball does not belong to requested brand");
        var doc = Jsoup.parse(html, normalized);
        Element canonical = doc.selectFirst("link[rel=canonical][href]");
        if (canonical != null) {
            normalized = BowwwlParser.normalize(canonical.absUrl("href"));
            if (!isBallUrl(normalized, brandSlug)) throw new IllegalArgumentException("Canonical ball brand does not match");
        }
        String name = text(doc.selectFirst("h1 .field--name-title"));
        if (name == null || name.length() > 255) throw new IllegalArgumentException("Ball title missing or too long");
        String ballImage = image(doc.selectFirst(".field--name-field-ball-image img[src]"));
        String coreImage = image(doc.selectFirst(".field--name-field-core .field--name-field-core-image img[src]"));
        if (ballImage == null || coreImage == null) throw new IllegalArgumentException("Ball and core images are required");
        return new Detail(name, normalized, ballImage, coreImage,
                text(doc.selectFirst(".field--name-field-core h5 a")),
                text(doc.selectFirst(".field--name-field-coverstock-type .field__item")),
                text(doc.selectFirst(".field--name-field-core .field--name-field-core-type .field__item")));
    }
    public static String brandUrl(String slug) {
        if (!FeaturedCatalog.BRAND_SLUGS.contains(slug)) throw new IllegalArgumentException("Unsupported featured brand");
        return BowwwlParser.ROOT + "/" + slug;
    }
    private static boolean isBallUrl(String url, String slug) {
        String prefix = brandUrl(slug) + "/";
        return url.startsWith(prefix) && url.substring(prefix.length()).matches("[a-z0-9]+(?:-[a-z0-9]+)*")
                && !Set.of("cores", "coverstocks", "balls").contains(url.substring(prefix.length()));
    }
    private static String image(Element img) {
        if (img == null) return null;
        try {
            URI uri = URI.create(img.baseUri()).resolve(img.attr("src")).normalize();
            String path = uri.getPath();
            if (!"https".equals(uri.getScheme()) || !"www.bowwwl.com".equals(uri.getHost())
                    || uri.getPort() != -1 || uri.getUserInfo() != null || uri.getFragment() != null
                    || !path.startsWith("/sites/default/files/")
                    || !path.toLowerCase(Locale.ROOT).matches(".*\\.(png|jpe?g|webp|gif|avif)$")
                    || uri.toASCIIString().length() > 2048) return null;
            return uri.toASCIIString();
        } catch (IllegalArgumentException ex) { return null; }
    }
    private static String text(Element element) {
        if (element == null || element.text().isBlank()) return null;
        String text = element.text().trim();
        return text.length() <= 255 ? text : null;
    }
}
