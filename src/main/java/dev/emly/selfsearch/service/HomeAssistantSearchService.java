package dev.emly.selfsearch.service;

import java.util.Map;

import org.springframework.web.reactive.function.client.WebClient;

import dev.emly.selfsearch.SearchResult;
import dev.emly.selfsearch.config.ServiceConfig;
import reactor.core.publisher.Flux;

//@Service
public class HomeAssistantSearchService implements SearchService {

	private final WebClient webClient;
	private boolean hasErrored;

	public HomeAssistantSearchService(WebClient webClient) {
		this.webClient = webClient;
	}

	@Override
	public boolean isWebSearch() {
		return false;
	}

	@Override
	public String getServiceName() {
		return "Home Assistant";
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<SearchResult> search(String query, ServiceConfig config) {
		this.hasErrored = false;

		String endpoint = String.format("%sapi/states", config.getUrl());

		return webClient.get().uri(endpoint).header("Authorization", "Bearer " + config.getApiKey()).retrieve()
				.bodyToFlux(Map.class).filter(state -> {
					String entityId = (String) state.get("entity_id");
					Map<String, Object> attributes = (Map<String, Object>) state.get("attributes");
					String friendlyName = attributes != null ? (String) attributes.get("friendly_name") : "";

					String q = query.toLowerCase();
					return (entityId != null && entityId.toLowerCase().contains(q))
							|| (friendlyName != null && friendlyName.toLowerCase().contains(q));
				}).map(state -> {
					String entityId = (String) state.get("entity_id");
					Map<String, Object> attributes = (Map<String, Object>) state.get("attributes");
					String friendlyName = attributes != null
							? (String) attributes.getOrDefault("friendly_name", entityId)
							: entityId;
					String currentState = (String) state.getOrDefault("state", "unknown");

					return SearchResult.builder().sourceService(this).title(friendlyName)
							.category("Entity (" + entityId.split("\\.")[0] + ")")
							.url(config.getUrl() + "history?entity_id=" + entityId)
							.description("Entity ID: " + entityId + " | Current State: " + currentState).build();
				}).onErrorResume(e -> {
					System.err.println(
							String.format("[%s] Error querying API: %s", this.getServiceName(), e.getMessage()));
					return Flux.empty();
				});
	}

	@Override
	public boolean hasErrored() {
		return this.hasErrored;
	}
}
