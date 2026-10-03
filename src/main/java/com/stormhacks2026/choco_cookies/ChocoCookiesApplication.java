package com.stormhacks2026.choco_cookies;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChocoCookiesApplication {

	public static void main(String[] args) {
		var context = SpringApplication.run(ChocoCookiesApplication.class, args);
        if (context.getEnvironment().getProperty("catalog.import", Boolean.class, false)) {
            context.close();
        }
	}
}
