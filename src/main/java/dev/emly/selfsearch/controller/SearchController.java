package dev.emly.selfsearch.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dev.emly.selfsearch.SearchResult;
import dev.emly.selfsearch.config.UserSettings;
import dev.emly.selfsearch.config.UserSettingsService;
import dev.emly.selfsearch.service.SearchService;
import reactor.core.publisher.Flux;

@Controller
public class SearchController {

	private final List<SearchService> searchServices;
	private final UserSettingsService userSettingsService;

	private SearchController(List<SearchService> searchServices, UserSettingsService userSettingsService) {
		this.searchServices = searchServices;
		this.userSettingsService = userSettingsService;
	}

	@GetMapping("/")
	public String index(Model model, @AuthenticationPrincipal OidcUser principal) {
		if (principal != null) {
			model.addAttribute("username", principal.getPreferredUsername());
		}
		return "index";
	}

	@GetMapping("/search")
	public String search(@RequestParam(name = "query", required = false) String query,
			@AuthenticationPrincipal OidcUser principal, Model model) {
		if (query == null || query.isBlank())
			return "redirect:/";

		String username = principal.getName();
		UserSettings settings = userSettingsService.getUserSettings(username);

		// 1. Authorization check based on mapped OIDC roles
		List<SearchService> enabledServices = searchServices.stream()
				.filter(service -> settings.getConfig(service.getServiceName()).isEnabled()).toList();

		// 2. Fetch all authorized services asynchronously
		Map<Boolean, List<SearchResult>> partitionedResults = Flux.fromIterable(enabledServices)
				.flatMap(service -> service.search(query, settings.getConfig(service.getServiceName()))).collectList()
				.block().stream().collect(Collectors.partitioningBy(res -> res.getSourceService().isWebSearch()));

		List<SearchResult> localResults = partitionedResults.get(false);
		List<SearchResult> webResults = partitionedResults.get(true);

		System.out.println("authed: " + enabledServices.stream().map(SearchService::getServiceName).toList());

		model.addAttribute("query", query);
		model.addAttribute("localResults", localResults);
		model.addAttribute("webResults", webResults);
		model.addAttribute("username", principal != null ? principal.getPreferredUsername() : "User");
		model.addAttribute("allowedServices", enabledServices.stream()
				.collect(Collectors.toMap(SearchService::getServiceName, SearchService::hasErrored)));

		return "index";
	}
}
