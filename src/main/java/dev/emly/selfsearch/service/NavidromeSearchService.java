package dev.emly.selfsearch.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import dev.emly.selfsearch.EnumAuthType;
import dev.emly.selfsearch.SearchResult;
import dev.emly.selfsearch.config.ServiceConfig;
import reactor.core.publisher.Flux;

@Service
public class NavidromeSearchService implements SearchService {

	private final WebClient webClient;
	private final MessageDigest md;
	private boolean hasErrored;

	public NavidromeSearchService(WebClient webClient) {
		this.webClient = webClient;
		MessageDigest tmp = null;
		try {
			tmp = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		this.md = tmp;
	}

	@Override
	public boolean isWebSearch() {
		return false;
	}

	@Override
	public String getServiceName() {
		return "Navidrome";
	}

	@SuppressWarnings("unchecked")
	@Override
	public Flux<SearchResult> search(String query, ServiceConfig config) {
		this.hasErrored = false;

		String salt = UUID.randomUUID().toString().substring(0, 8);
		String token = HexFormat.of().formatHex(md.digest((config.getApiKey() + salt).getBytes()));

		String endpoint = String.format("%srest/search3?query=%s&u=%s&t=%s&s=%s&v=1.16.1&c=UnifiedSearch&f=json",
				config.getUrl(), query, config.getServiceUsername(), token, salt);

		return webClient.get().uri(endpoint).retrieve().bodyToMono(Map.class).flatMapMany(response -> {
			Map<String, Object> subsonic = (Map<String, Object>) response.get("subsonic-response");
			if (subsonic == null || !subsonic.containsKey("searchResult3"))
				return Flux.empty();

			Map<String, Object> searchResult3 = (Map<String, Object>) subsonic.get("searchResult3");
			List<SearchResult> results = new ArrayList<>();

			List<Map<String, Object>> songs = (List<Map<String, Object>>) searchResult3.get("song");
			if (songs != null) {
				for (var song : songs) {
					String coverArtId = (String) song.getOrDefault("coverArt", song.get("id"));
					String imageUrl = null;

					if (coverArtId != null && !coverArtId.isBlank()) {
						imageUrl = String.format(
								"%srest/getCoverArt?id=%s&size=160&u=%s&t=%s&s=%s&v=1.16.1&c=UnifiedSearch",
								config.getUrl(), coverArtId, config.getServiceUsername(), token, salt);
					}

					results.add(SearchResult.builder().sourceService(this).title((String) song.get("title"))
							.category("Track").url(config.getUrl() + "app/#/album/" + song.get("albumId") + "/show")
							.imageUrl(imageUrl)
							.description("Artist: " + song.get("artist") + " | Album: " + song.get("album")).build());
				}
			}
			return Flux.fromIterable(results);
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

	@Override
	public EnumAuthType getAuthType() {
		return EnumAuthType.USER_AND_PASS;
	}
}