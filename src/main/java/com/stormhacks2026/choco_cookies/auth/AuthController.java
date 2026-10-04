package com.stormhacks2026.choco_cookies.auth;

import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    public AuthController(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }
    @GetMapping("/login") String login() { return "login"; }
    @GetMapping("/register") String register(Model model) {
        model.addAttribute("registration", new RegistrationForm());
        return "register";
    }
    @PostMapping("/register") String register(@Valid @ModelAttribute("registration") RegistrationForm form, BindingResult errors) {
        String username = form.getUsername().toLowerCase(Locale.ROOT);
        if (!errors.hasErrors() && users.findByUsername(username).isPresent()) {
            errors.rejectValue("username", "duplicate", "That username is already taken.");
        }
        if (!errors.hasErrors()) {
            try {
                users.saveAndFlush(new AppUser(username, encoder.encode(form.getPassword())));
                return "redirect:/login?registered";
            } catch (DataIntegrityViolationException ex) {
                errors.rejectValue("username", "duplicate", "That username is already taken.");
            }
        }
        form.setPassword("");
        return "register";
    }
}
