package com.stormhacks2026.choco_cookies.auth;

import java.security.Principal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserAdvice {
    @ModelAttribute("currentUsername") public String currentUsername(Principal principal) {
        return principal == null ? null : principal.getName();
    }
}
