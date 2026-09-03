package dev.emly.selfsearch.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.emly.selfsearch.EnumAuthType;
import dev.emly.selfsearch.SearchResult;
import dev.emly.selfsearch.config.ServiceConfig;
import reactor.core.publisher.Flux;

@Service
public class DuckDuckGoSearchService implements SearchService {

	private final WebClient webClient;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private boolean hasErrored;

	public DuckDuckGoSearchService(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.baseUrl("https://api.duckduckgo.com").build();
	}

	@Override
	public boolean isWebSearch() {
		return true;
	}

	@Override
	public String getServiceName() {
		return "DuckDuckGo";
	}

	@Override
	public Flux<SearchResult> search(String query, ServiceConfig config) {
		this.hasErrored = false;
		return webClient.get()
				.uri(uriBuilder -> uriBuilder.path("/").queryParam("q", query).queryParam("format", "json")
						.queryParam("no_html", "1").queryParam("skip_disambig", "1").build())
				.retrieve().bodyToMono(String.class).flatMapMany(rawJson -> {
					try {
						JsonNode root = objectMapper.readTree(rawJson);
						return parseDuckDuckGoResponse(root);
					} catch (Exception e) {
						System.err.println(
								String.format("[%s] Error parsing JSON: %s", this.getServiceName(), e.getMessage()));
						return Flux.empty();
					}
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
		return EnumAuthType.NONE;
	}

	private Flux<SearchResult> parseDuckDuckGoResponse(JsonNode root) {
		List<SearchResult> results = new ArrayList<>();

		// 1. Check for Direct Answer / Abstract Image
		String abstractText = root.path("AbstractText").asText("");
		String abstractUrl = root.path("AbstractURL").asText("");
		String abstractImage = extractImageUrl(root.path("Image").asText(""));

		if (!abstractText.isBlank() && !abstractUrl.isBlank()) {
			results.add(SearchResult.builder()
					.title(abstractText.length() > 80 ? abstractText.substring(0, 80) + "..." : abstractText)
					.description(abstractText).url(abstractUrl).imageUrl(abstractImage).sourceService(this).build());
		}

		// 2. Check for Answer Box (e.g., math, unit conversions)
		String answer = root.path("Answer").asText("");
		if (!answer.isBlank()) {
			results.add(SearchResult.builder().title("Instant Answer: " + answer).description(answer)
					.url("https://duckduckgo.com").sourceService(this).build());
		}

		// 3. Process Related Topics & Icon images
		JsonNode relatedTopics = root.path("RelatedTopics");
		if (relatedTopics.isArray()) {
			for (JsonNode topic : relatedTopics) {
				if (topic.has("Text") && topic.has("FirstURL")) {
					String text = topic.path("Text").asText();
					String url = topic.path("FirstURL").asText();

					// Extract icon image URL if present
					String iconUrl = "";
					if (topic.has("Icon")) {
						iconUrl = topic.path("Icon").path("URL").asText("");
					}
					String imageUrl = extractImageUrl(iconUrl);

					results.add(SearchResult.builder().title(extractTitle(text)).description(text).url(url)
							.imageUrl(imageUrl).sourceService(this).build());
				}
			}
		}

		return Flux.fromIterable(results);
	}

	private String extractImageUrl(String rawUrl) {
		if (rawUrl == null || rawUrl.isBlank()) {
			return null;
		}
		if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
			return rawUrl;
		}
		// Convert DDG relative asset paths (e.g. "/i/3a8c1f.jpg") into absolute URLs
		return "https://duckduckgo.com" + (rawUrl.startsWith("/") ? rawUrl : "/" + rawUrl);
	}

	private String extractTitle(String text) {
		if (text.contains(" - ")) {
			return text.split(" - ")[0];
		}
		return text.length() > 60 ? text.substring(0, 60) + "..." : text;
	}
}