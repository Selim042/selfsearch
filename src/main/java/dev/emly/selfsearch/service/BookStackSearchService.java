package dev.emly.selfsearch.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import dev.emly.selfsearch.EnumAuthType;
import dev.emly.selfsearch.SearchResult;
import dev.emly.selfsearch.config.ServiceConfig;
import reactor.core.publisher.Flux;

@Service
public class BookStackSearchService implements SearchService {

	private final WebClient webClient;
	private boolean hasErrored;

	public BookStackSearchService(WebClient webClient) {
		this.webClient = webClient;
	}

	@Override
	public boolean isWebSearch() {
		return false;
	}

	@Override
	public String getServiceName() {
		return "BookStack";
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<SearchResult> search(String query, ServiceConfig config) {
		this.hasErrored = false;

		String endpoint = String.format("%sapi/search?query=%s", config.getUrl(), query);

		return webClient.get().uri(endpoint)
				.header("Authorization", "Token " + config.getServiceUsername() + ":" + config.getApiKey()).retrieve()
				.bodyToMono(Map.class).flatMapMany(response -> {
					List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
					if (data == null)
						return Flux.empty();
					return Flux.fromIterable(data)
							.map(item -> SearchResult.builder().sourceService(this).title((String) item.get("name"))
									.category((String) item.get("type")).url((String) item.get("url"))
//									.description((String) item.get("preview_html"))
									.build());
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

	@Override
	public EnumAuthType getAuthType() {
		return EnumAuthType.ID_AND_SECRET;
	}
}
