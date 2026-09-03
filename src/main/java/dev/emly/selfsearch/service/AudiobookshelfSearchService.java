package dev.emly.selfsearch.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import dev.emly.selfsearch.SearchResult;
import dev.emly.selfsearch.config.ServiceConfig;
import reactor.core.publisher.Flux;

@Service
public class AudiobookshelfSearchService implements SearchService {

	private final WebClient webClient;
	private boolean hasErrored;

	public AudiobookshelfSearchService(WebClient webClient) {
		this.webClient = webClient;
	}

	@Override
	public boolean isWebSearch() {
		return false;
	}

	@Override
	public String getServiceName() {
		return "Audiobookshelf";
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<SearchResult> search(String query, ServiceConfig config) {
		this.hasErrored = false;
		String baseUrl = config.getUrl().endsWith("/") ? config.getUrl() : config.getUrl() + "/";

		// 1. Fetch available libraries first to get active Library IDs
		return webClient.get().uri(baseUrl + "api/libraries").header("Authorization", "Bearer " + config.getApiKey())
				.retrieve().bodyToMono(Map.class).flatMapMany(librariesResponse -> {
					List<Map<String, Object>> libraries = (List<Map<String, Object>>) librariesResponse
							.get("libraries");
					if (libraries == null || libraries.isEmpty())
						return Flux.empty();

					// 2. Query each library's search endpoint: /api/libraries/{id}/search?q=query
					return Flux.fromIterable(libraries).flatMap(library -> {
						String libraryId = (String) library.get("id");
						String searchUrl = String.format("%sapi/libraries/%s/search?q=%s", baseUrl, libraryId, query);

						return webClient.get().uri(searchUrl).header("Authorization", "Bearer " + config.getApiKey())
								.retrieve().bodyToMono(Map.class)
								.flatMapMany(searchResponse -> parseSearchResults(searchResponse, baseUrl,
										config.getApiKey()));
					});
				}).onErrorResume(e -> {
					// TODO: figure out why I need to cast e??
					this.hasErrored = true;
					System.err.println(String.format("[%s] Error querying API: %s", this.getServiceName(),
							((Exception) e).getMessage()));
					return Flux.empty();
				});
	}

	@SuppressWarnings("unchecked")
	private Flux<SearchResult> parseSearchResults(Map<String, Object> response, String baseUrl, String apiKey) {
		List<SearchResult> results = new ArrayList<>();

		// Response contains arrays for "book", "podcast", etc.
		List<Map<String, Object>> bookMatches = (List<Map<String, Object>>) response.get("book");

		if (bookMatches != null) {
			for (var entry : bookMatches) {
				Map<String, Object> item = entry.containsKey("libraryItem")
						? (Map<String, Object>) entry.get("libraryItem")
						: entry;

				if (item == null)
					continue;

				String itemId = (String) item.get("id");
				Map<String, Object> media = (Map<String, Object>) item.get("media");
				Map<String, Object> metadata = media != null ? (Map<String, Object>) media.get("metadata") : null;

				String title = metadata != null && metadata.get("title") != null ? (String) metadata.get("title")
						: "Unknown Title";
				String author = metadata != null && metadata.get("authorName") != null
						? (String) metadata.get("authorName")
						: "Unknown Author";

				// Construct proxied cover image URL
				String proxiedImageUrl = null;
				if (itemId != null && !itemId.isBlank()) {
					String directCoverUrl = String.format("%sapi/items/%s/cover", baseUrl, itemId);
					String headersJson = String.format("{\"Authorization\":\"Bearer %s\"}", apiKey);
					String encodedHeaders = Base64.getUrlEncoder()
							.encodeToString(headersJson.getBytes(StandardCharsets.UTF_8));

					proxiedImageUrl = String.format("/api/proxy/image?url=%s&headers=%s",
							URLEncoder.encode(directCoverUrl, StandardCharsets.UTF_8), encodedHeaders);
				}

				results.add(SearchResult.builder().sourceService(this).title(title).category("Audiobook")
						.url(baseUrl + "item/" + itemId).imageUrl(proxiedImageUrl).description("Author: " + author)
						.build());
			}
		}

		return Flux.fromIterable(results);
	}

	@Override
	public boolean hasErrored() {
		return this.hasErrored;
	}
}