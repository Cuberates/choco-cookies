package com.stormhacks2026.choco_cookies.catalog;

import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CatalogController {
    private final BowlingBallRepository repository;
    private final CatalogImporter importer;
    public CatalogController(BowlingBallRepository repository, CatalogImporter importer) {
        this.repository = repository; this.importer = importer;
    }

    @GetMapping("/catalog/search")
    public String search(@RequestParam(defaultValue = "") String name,
                         @RequestParam(required = false) Integer weight,
                         @RequestParam(defaultValue = "") String coverstockType,
                         @RequestParam(defaultValue = "") String coreType,
                         @RequestParam(defaultValue = "") String brand,
                         @RequestParam(defaultValue = "name") String sort,
                         org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        name = name.trim(); brand = brand.trim();
        try {
            if (!name.isBlank() && !repository.exists(filters(name, null, "", "", brand))) {
                if (brand.isBlank()) {
                    redirect.addFlashAttribute("lookupMessage", "Enter a brand and the full ball name to look it up on Bowwwl.");
                } else {
                    String url = CatalogImporter.ballUrl(brand, name);
                    var existing = repository.findBySourceKey(url);
                    if (existing.isEmpty()) {
                        var report = importer.runBall(brand, name);
                        redirect.addFlashAttribute("lookupMessage", report.malformed() > 0
                                ? "Bowwwl returned a page without usable ball data. Check the brand and full name."
                                : "Ball added from Bowwwl. Selected filters still apply.");
                        existing = repository.findBySourceKey(url);
                    }
                    if (existing.isPresent()) {
                        name = existing.get().getName();
                        brand = existing.get().getBrand() == null ? "" : existing.get().getBrand();
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("lookupMessage", "Use the full Bowwwl ball name and brand with letters, numbers, spaces, or hyphens (for example, Storm / Phaze II).");
        } catch (java.io.IOException ex) {
            redirect.addFlashAttribute("lookupMessage", "Could not retrieve this ball from Bowwwl. Check the brand and full name, or try again later.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            redirect.addFlashAttribute("lookupMessage", "The Bowwwl lookup was interrupted. Please try again.");
        } catch (org.springframework.dao.DataAccessException ex) {
            redirect.addFlashAttribute("lookupMessage", "Could not save the ball. Please try again later.");
        }
        redirect.addAttribute("name", name); redirect.addAttribute("brand", brand);
        redirect.addAttribute("weight", weight); redirect.addAttribute("coverstockType", coverstockType);
        redirect.addAttribute("coreType", coreType); redirect.addAttribute("sort", sort);
        return "redirect:/catalog";
    }

    @GetMapping("/")
    public String home() { return "redirect:/catalog"; }

    @GetMapping("/catalog")
    @Transactional(readOnly = true)
    public String catalog(@RequestParam(defaultValue = "") String name,
                          @RequestParam(required = false) Integer weight,
                          @RequestParam(defaultValue = "") String coverstockType,
                          @RequestParam(defaultValue = "") String coreType,
                          @RequestParam(defaultValue = "") String brand,
                          @RequestParam(defaultValue = "name") String sort,
                          @RequestParam(defaultValue = "0") int page, Model model) {
        name = name.trim();
        sort = "name-desc".equals(sort) ? "name-desc" : "name";
        var order = "name-desc".equals(sort) ? Sort.Direction.DESC : Sort.Direction.ASC;
        var specification = filters(name, weight, coverstockType, coreType, brand);
        var ordering = Sort.by(order, "name").and(Sort.by("id"));
        var results = repository.findAll(specification, PageRequest.of(Math.max(0, page), 20, ordering));
        if (page > 0 && page >= results.getTotalPages()) {
            results = repository.findAll(specification,
                    PageRequest.of(Math.max(0, results.getTotalPages() - 1), 20, ordering));
        }
        // Materialize lazy weight collections while the read transaction is open.
        model.addAttribute("balls", results.getContent().stream().map(ball -> new BallView(
                ball.getName(), ball.getBrand(), ball.getCoverstockType(), ball.getCoreType(),
                ball.getWeights().stream().sorted().toList(), ball.getSourceUrl())).toList());
        model.addAttribute("results", results);
        model.addAttribute("name", name); model.addAttribute("weight", weight);
        model.addAttribute("coverstockType", coverstockType); model.addAttribute("coreType", coreType);
        model.addAttribute("brand", brand); model.addAttribute("sort", sort);
        model.addAttribute("brands", repository.findBrands());
        model.addAttribute("coverstockTypes", repository.findCoverstockTypes());
        model.addAttribute("coreTypes", repository.findCoreTypes());
        model.addAttribute("weights", repository.findWeights());
        return "catalog";
    }

    static Specification<BowlingBall> filters(String name, Integer weight, String coverstock, String core, String brand) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!name.isBlank()) {
                String escaped = name.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + escaped + "%", '\\'));
            }
            if (weight != null) predicates.add(cb.isMember(weight, root.get("weights")));
            if (!coverstock.isEmpty()) predicates.add(cb.equal(root.get("coverstockType"), coverstock));
            if (!core.isEmpty()) predicates.add(cb.equal(root.get("coreType"), core));
            if (!brand.isEmpty()) predicates.add(cb.equal(root.get("brand"), brand));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    public record BallView(String name, String brand, String coverstockType, String coreType, 
                           java.util.List<Integer> weights, String sourceUrl) {}
}
