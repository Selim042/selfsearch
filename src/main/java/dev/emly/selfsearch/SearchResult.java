package dev.emly.selfsearch;

import dev.emly.selfsearch.service.SearchService;

public class SearchResult {
	private SearchService sourceService;
	private String title;
	private String category;
	private String url;
	private String description;
	private String imageUrl;

	private SearchResult(SearchService sourceService, String title, String category, String url, String description,
			String imageUrl) {
		this.sourceService = sourceService;
		this.title = title;
		this.category = category;
		this.url = url;
		this.description = description;
		this.imageUrl = imageUrl;
	}

	public SearchService getSourceService() {
		return sourceService;
	}

	public String getTitle() {
		return title;
	}

	public String getCategory() {
		return category;
	}

	public String getUrl() {
		return url;
	}

	public String getDescription() {
		return description;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public static SearchResult.Builder builder() {
		return new SearchResult.Builder();
	}

	public static class Builder {
		private SearchService sourceService;
		private String title;
		private String category;
		private String url;
		private String description;
		private String imageUrl;

		private Builder() {
		}

		public SearchResult.Builder sourceService(SearchService sourceService) {
			this.sourceService = sourceService;
			return this;
		}

		public SearchResult.Builder title(String title) {
			this.title = title;
			return this;
		}

		public SearchResult.Builder category(String category) {
			this.category = category;
			return this;
		}

		public SearchResult.Builder url(String url) {
			this.url = url;
			return this;
		}

		public SearchResult.Builder description(String desc) {
			this.description = desc;
			return this;
		}

		public SearchResult.Builder imageUrl(String imageUrl) {
			this.imageUrl = imageUrl;
			return this;
		}

		public SearchResult build() {
			return new SearchResult(sourceService, title, category, url, description, imageUrl);
		}
	}
}