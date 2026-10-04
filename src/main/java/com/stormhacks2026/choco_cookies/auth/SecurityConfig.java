package com.stormhacks2026.choco_cookies.auth;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean DefaultCookieSerializer cookieSerializer(@Value("${server.servlet.session.cookie.secure:#{null}}") Boolean secure) {
        var serializer = new DefaultCookieSerializer();
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        serializer.setCookiePath("/");
        if (secure != null) serializer.setUseSecureCookie(secure);
        return serializer;
    }

    @Bean UserDetailsService userDetailsService(AppUserRepository users) {
        return username -> {
            var user = users.findByUsername(username.trim().toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
            return User.withUsername(user.getUsername()).password(user.getPasswordHash()).roles("USER").build();
        };
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/journal", "/journal/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/journal", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").deleteCookies("SESSION"))
                .build();
    }
}
