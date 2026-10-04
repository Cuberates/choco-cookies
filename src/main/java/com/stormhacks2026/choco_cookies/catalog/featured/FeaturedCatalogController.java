package com.stormhacks2026.choco_cookies.catalog.featured;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FeaturedCatalogController {
    private final FeaturedCatalog featured;
    public FeaturedCatalogController(FeaturedCatalog featured) { this.featured = featured; }
    @PostMapping("/catalog/featured/refresh") String refresh(RedirectAttributes redirect) {
        redirect.addFlashAttribute("featuredMessage", featured.refresh().message());
        return "redirect:/catalog#featured-showcase";
    }
}
