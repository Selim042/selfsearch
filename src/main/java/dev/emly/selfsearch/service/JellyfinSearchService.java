package dev.emly.selfsearch.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import dev.emly.selfsearch.SearchResult;
import dev.emly.selfsearch.config.ServiceConfig;
import reactor.core.publisher.Flux;

@Service
public class JellyfinSearchService implements SearchService {

	private final WebClient webClient;
	private boolean hasErrored;

	public JellyfinSearchService(WebClient webClient) {
		this.webClient = webClient;
	}

	@Override
	public boolean isWebSearch() {
		return false;
	}

	@Override
	public String getServiceName() {
		return "Jellyfin";
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<SearchResult> search(String query, ServiceConfig config) {
		this.hasErrored = false;

		String endpoint = String.format("%s/Items?searchTerm=%s&limit=10&recursive=true", config.getUrl(), query);

		return webClient.get().uri(endpoint).header("X-Emby-Token", config.getApiKey()).retrieve().bodyToMono(Map.class)
				.flatMapMany(response -> {
					List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("Items");
					if (items == null)
						return Flux.empty();
					return Flux.fromIterable(items).map(item -> {
						String itemId = (String) item.get("Id");
						Map<String, Object> imageTags = (Map<String, Object>) item.get("ImageTags");

						String imageUrl = null;
						String proxiedImageUrl = null;
						if (imageTags != null && imageTags.containsKey("Primary")) {
							imageUrl = String.format(
									"%sItems/%s/Images/Primary?fillWidth=120&fillHeight=160&quality=90",
									config.getUrl(), itemId);
							proxiedImageUrl = "/api/proxy/image?url="
									+ URLEncoder.encode(imageUrl, StandardCharsets.UTF_8);
						}

						return SearchResult.builder().sourceService(this).title((String) item.get("Name"))
								.category((String) item.get("Type")).url(config.getUrl() + "#/details?id=" + itemId)
								.imageUrl(proxiedImageUrl).description("Overview: " + item.getOrDefault("Overview", "N/A"))
								.build();
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