package dev.emly.selfsearch.controller;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Controller
public class ImageProxyController {

	private final WebClient proxyWebClient;
	private final ObjectMapper objectMapper;

	public ImageProxyController(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.proxyWebClient = WebClient.builder()
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
																											// limit
						.build())
				.build();
	}

	@GetMapping("/api/proxy/image")
	@ResponseBody
	public Mono<ResponseEntity<byte[]>> proxyImage(@RequestParam("url") String rawTargetUrl,
			@RequestParam(value = "headers", required = false) String encodedHeaders) {

		try {
			URI targetUri = URI.create(rawTargetUrl);

			return proxyWebClient.get().uri(targetUri).headers(httpHeaders -> {
				httpHeaders.set(HttpHeaders.USER_AGENT, "HomelabUnifiedSearch/1.0");

				// If optional Base64 JSON headers parameter is provided, decode and attach
				if (encodedHeaders != null && !encodedHeaders.isBlank()) {
					try {
						String json = new String(Base64.getUrlDecoder().decode(encodedHeaders), StandardCharsets.UTF_8);
						Map<String, String> headerMap = objectMapper.readValue(json,
								new TypeReference<Map<String, String>>() {
								});
						headerMap.forEach(httpHeaders::set);
					} catch (Exception e) {
						System.err.println("[ImageProxy] Failed to parse custom headers: " + e.getMessage());
					}
				}
			}).exchangeToMono(response -> {
				if (response.statusCode().is2xxSuccessful()) {
					MediaType contentType = response.headers().contentType().orElse(MediaType.IMAGE_JPEG);

					return response.bodyToMono(byte[].class).map(bytes -> ResponseEntity.ok().contentType(contentType)
							.header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400").body(bytes));
				}

				System.err.println(String.format("[ImageProxy] Upstream HTTP %s for URL: %s", response.statusCode(),
						rawTargetUrl));
				return Mono.just(ResponseEntity.status(response.statusCode()).build());
			});
		} catch (Exception e) {
			System.err.println("[ImageProxy] Exception fetching image: " + e.getMessage());
			return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
		}
	}
}