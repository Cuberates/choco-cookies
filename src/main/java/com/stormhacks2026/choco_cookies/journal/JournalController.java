package com.stormhacks2026.choco_cookies.journal;

import com.stormhacks2026.choco_cookies.catalog.BowlingBallRepository;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/journal")
public class JournalController {
    private final JournalService journal;
    private final BowlingBallRepository balls;
    public JournalController(JournalService journal, BowlingBallRepository balls) { this.journal = journal; this.balls = balls; }
    @InitBinder("entry") void bind(WebDataBinder binder) {
        binder.setAllowedFields("bowlingDate", "leagueName", "alleyName", "location", "games", "pins", "notes", "ballIds", "ballIds[*]");
    }
    @GetMapping String index(Principal principal, Model model) {
        var entries = journal.list(principal.getName());
        model.addAttribute("entries", entries);
        model.addAttribute("stats", JournalStats.from(entries));
        return "journal";
    }
    @GetMapping("/new") String create(Model model) {
        model.addAttribute("entry", new JournalForm());
        return form(model, null);
    }
    @GetMapping("/{id}/edit") String edit(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("entry", JournalForm.from(journal.owned(id, principal.getName())));
        return form(model, id);
    }
    @PostMapping String create(@Valid @ModelAttribute("entry") JournalForm entry, BindingResult errors, Principal principal, Model model) {
        return save(null, entry, errors, principal, model);
    }
    @PostMapping("/{id}") String edit(@PathVariable Long id, @Valid @ModelAttribute("entry") JournalForm entry,
                                    BindingResult errors, Principal principal, Model model) {
        journal.owned(id, principal.getName());
        return save(id, entry, errors, principal, model);
    }
    private String save(Long id, JournalForm entry, BindingResult errors, Principal principal, Model model) {
        if (!errors.hasErrors()) {
            try { journal.save(id, principal.getName(), entry); }
            catch (IllegalArgumentException ex) { errors.rejectValue("ballIds", "invalid", ex.getMessage()); }
        }
        if (errors.hasErrors()) return form(model, id);
        return "redirect:/journal?saved";
    }
    private String form(Model model, Long id) {
        model.addAttribute("entryId", id);
        model.addAttribute("catalogBalls", balls.findAll(Sort.by("brand", "name")));
        model.addAttribute("today", LocalDate.now());
        return "journal-form";
    }
    @PostMapping("/{id}/delete") String delete(@PathVariable Long id, Principal principal) {
        journal.delete(id, principal.getName());
        return "redirect:/journal?deleted";
    }
}
