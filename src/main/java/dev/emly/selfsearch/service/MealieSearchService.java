package dev.emly.selfsearch.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import dev.emly.selfsearch.EnumAuthType;
import dev.emly.selfsearch.SearchResult;
import dev.emly.selfsearch.config.ServiceConfig;
import reactor.core.publisher.Flux;

@Service
public class MealieSearchService implements SearchService {

	private final WebClient webClient;
	private boolean hasErrored;

	public MealieSearchService(WebClient webClient) {
		this.webClient = webClient;
	}

	@Override
	public boolean isWebSearch() {
		return false;
	}

	@Override
	public String getServiceName() {
		return "Mealie";
	}

	@Override
	public EnumAuthType getAuthType() {
		return EnumAuthType.API_KEY;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<SearchResult> search(String query, ServiceConfig config) {
		this.hasErrored = false;
		String baseUrl = config.getUrl().endsWith("/") ? config.getUrl() : config.getUrl() + "/";

		// Mealie v1.x search endpoint
		String endpoint = String.format("%sapi/recipes?search=%s&perPage=10", baseUrl,
				URLEncoder.encode(query, StandardCharsets.UTF_8));

		return webClient.get().uri(endpoint).header("Authorization", "Bearer " + config.getApiKey()).retrieve()
				.bodyToMono(Map.class).flatMapMany(response -> {
					List<SearchResult> results = new ArrayList<>();

					// Mealie wraps recipe items inside "items" list
					List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

					if (items != null) {
						for (var item : items) {
							String recipeId = (String) item.get("id");
							String slug = (String) item.get("slug");
							String name = (String) item.getOrDefault("name", "Unnamed Recipe");
							String description = (String) item.getOrDefault("description", "");

							// Construct proxied recipe image URL
							String proxiedImageUrl = null;
							if (recipeId != null && !recipeId.isBlank()) {
								String directImageUrl = String.format("%sapi/media/recipes/%s/images/min-original.webp",
										baseUrl, recipeId);
								String headersJson = String.format("{\"Authorization\":\"Bearer %s\"}",
										config.getApiKey());
								String encodedHeaders = Base64.getUrlEncoder()
										.encodeToString(headersJson.getBytes(StandardCharsets.UTF_8));

								proxiedImageUrl = String.format("/api/proxy/image?url=%s&headers=%s",
										URLEncoder.encode(directImageUrl, StandardCharsets.UTF_8), encodedHeaders);
							}

							results.add(SearchResult.builder().sourceService(this).title(name).category("Recipe")
									.url(baseUrl + "g/home/r/" + slug).imageUrl(proxiedImageUrl)
									.description(description.length() > 120 ? description.substring(0, 120) + "..."
											: description)
									.build());
						}
					}
					return Flux.fromIterable(results);
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