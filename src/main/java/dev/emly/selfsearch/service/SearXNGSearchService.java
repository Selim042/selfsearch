package dev.emly.selfsearch.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import dev.emly.selfsearch.SearchResult;
import dev.emly.selfsearch.config.ServiceConfig;
import reactor.core.publisher.Flux;

@Service
public class SearXNGSearchService implements SearchService {

	private final WebClient webClient;
	private boolean hasErrored;

	public SearXNGSearchService(WebClient webClient) {
		this.webClient = webClient;
	}

	@Override
	public boolean isWebSearch() {
		return true;
	}

	@Override
	public String getServiceName() {
		return "SearXNG";
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<SearchResult> search(String query, ServiceConfig config) {
		this.hasErrored = false;

		// SearXNG standard API endpoint with JSON formatting
		String endpoint = String.format("%s/search?q=%s&format=json", config.getUrl(), query);

		WebClient.RequestHeadersSpec<?> request = webClient.get().uri(endpoint);

		// Include API key if your SearXNG instance is behind an API proxy or secret key
		if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
			request.header("Authorization", "Bearer " + config.getApiKey());
		}

		return request.retrieve().bodyToMono(Map.class).flatMapMany(response -> {
			List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
			if (results == null)
				return Flux.empty();

			return Flux.fromIterable(results).take(10) // Limit top web search results
					.map(item -> SearchResult.builder().sourceService(this).title((String) item.get("title"))
							.category((String) item.getOrDefault("engine", "Web")).url((String) item.get("url"))
							.description((String) item.getOrDefault("content", "")).build());
		}).onErrorResume(e -> {
			this.hasErrored = true;
			System.err.println(String.format("[%s] Error querying API: %s", this.getServiceName(), e.getMessage()));
			return Flux.empty();
		});
	}

	@Override
	public boolean hasErrored() {
		return this.hasErrored;
	}
}
