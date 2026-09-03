package dev.emly.selfsearch.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import dev.emly.selfsearch.config.UserSettings;
import dev.emly.selfsearch.config.UserSettingsService;

@Controller
@RequestMapping("/settings")
public class SettingsController {

	private final UserSettingsService settingsService;

	public SettingsController(UserSettingsService settingsService) {
		this.settingsService = settingsService;
	}

	@GetMapping
	public String showSettings(@AuthenticationPrincipal OidcUser principal, Model model) {
		String username = principal.getName();
		UserSettings settings = settingsService.getUserSettings(username);

		model.addAttribute("userSettings", settings);
		return "settings";
	}

	@PostMapping
	public String saveSettings(@AuthenticationPrincipal OidcUser principal, @ModelAttribute UserSettings userSettings) {
		String username = principal.getName();
		settingsService.saveUserSettings(username, userSettings);
		return "redirect:/settings?saved=true";
	}
}