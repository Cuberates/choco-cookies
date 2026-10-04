package com.stormhacks2026.choco_cookies.catalog.featured;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "catalog.featured.refresh", havingValue = "true")
public class FeaturedCatalogCommand implements ApplicationRunner {
    private final FeaturedCatalog featured;
    public FeaturedCatalogCommand(FeaturedCatalog featured) { this.featured = featured; }
    @Override public void run(ApplicationArguments args) { System.out.println(featured.refresh().message()); }
}
