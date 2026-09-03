package dev.emly.selfsearch.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import dev.emly.selfsearch.SearchResult;
import dev.emly.selfsearch.config.ServiceConfig;
import reactor.core.publisher.Flux;

@Service
public class ImmichSearchService implements SearchService {

	private final WebClient webClient;
	private boolean hasErrored;

	public ImmichSearchService(WebClient webClient) {
		this.webClient = webClient;
	}

	@Override
	public boolean isWebSearch() {
		return false;
	}

	@Override
	public String getServiceName() {
		return "Immich";
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<SearchResult> search(String query, ServiceConfig config) {
		this.hasErrored = false;

		String endpoint = String.format("%sapi/search/metadata", config.getUrl());

		return webClient.post().uri(endpoint).header("x-api-key", config.getApiKey())
				.bodyValue(Map.of("originalFileName", query)).retrieve().bodyToMono(Map.class).flatMapMany(response -> {
					Map<String, Object> assets = (Map<String, Object>) response.get("assets");
					if (assets == null)
						return Flux.empty();
					List<Map<String, Object>> items = (List<Map<String, Object>>) assets.get("items");
					if (items == null)
						return Flux.empty();

					return Flux.fromIterable(items).map(item -> {
						String assetId = (String) item.get("id");
						String imageUrl = null;
						String proxiedImageUrl = null;

						if (assetId != null && !assetId.isBlank()) {
							// Example inside ImmichSearchService / NavidromeSearchService /
							// JellyfinSearchService
							imageUrl = String.format("%sapi/assets/%s/thumbnail?x-api-key=%s&format=JPEG",
									config.getUrl(), assetId, config.getApiKey());
							String headersJson = String.format("{\"x-api-key\":\"%s\"}", config.getApiKey());
							String encodedHeaders = Base64.getUrlEncoder()
									.encodeToString(headersJson.getBytes(StandardCharsets.UTF_8));

							// Construct full proxied URL
							proxiedImageUrl = String.format("/api/proxy/image?url=%s&headers=%s",
									URLEncoder.encode(imageUrl, StandardCharsets.UTF_8), encodedHeaders);
						}

						return SearchResult.builder().sourceService(this)
								.title((String) item.getOrDefault("originalFileName", "Unnamed Asset"))
								.category((String) item.getOrDefault("type", "Asset"))
								.url(config.getUrl() + "photos/" + assetId).imageUrl(proxiedImageUrl)
								.description("Asset ID: " + assetId).build();
					});
				}).onErrorResume(e -> {
					this.hasErrored = true;
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