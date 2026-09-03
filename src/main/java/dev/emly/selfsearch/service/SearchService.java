package dev.emly.selfsearch.service;

import dev.emly.selfsearch.EnumAuthType;
import dev.emly.selfsearch.SearchResult;
import dev.emly.selfsearch.config.ServiceConfig;
import reactor.core.publisher.Flux;

public interface SearchService {

	boolean isWebSearch();

	String getServiceName();

	Flux<SearchResult> search(String query, ServiceConfig config);

	boolean hasErrored();

	default EnumAuthType getAuthType() {
		return EnumAuthType.API_KEY;
	}
}
