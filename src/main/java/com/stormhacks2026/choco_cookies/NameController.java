package com.stormhacks2026.choco_cookies;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class NameController {

	@GetMapping("/name={name}")
	public String showName(@PathVariable String name, Model model) {
		model.addAttribute("name", name);
		return "name";
	}
}
