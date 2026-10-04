package com.stormhacks2026.choco_cookies.auth;

import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;

public class RegistrationForm {
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{3,40}", message = "Use 3–40 letters, numbers, underscores, or hyphens.")
    private String username = "";
    @NotBlank @Size(min = 8, max = 72, message = "Use a password of 8–72 characters (at most 72 UTF-8 bytes).")
    private String password = "";

    @AssertTrue(message = "Password must be at most 72 UTF-8 bytes.")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username == null ? "" : username.trim(); }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
