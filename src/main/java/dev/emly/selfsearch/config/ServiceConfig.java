package dev.emly.selfsearch.config;

import dev.emly.selfsearch.EnumAuthType;

public class ServiceConfig {
	private String serviceName;
	private String url;
	private String username;
	private String serviceUsername;
	private String apiKey;
	private boolean enabled;

	private EnumAuthType authType;

	public ServiceConfig() {
	}

	public ServiceConfig(String serviceName, String url, String username, String serviceUsername, String apiKey,
			boolean enabled) {
		this.serviceName = serviceName;
		this.url = url;
		this.username = username;
		this.serviceUsername = serviceUsername;
		this.apiKey = apiKey;
		this.enabled = enabled;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getUrl() {
		if (url == null || url.isEmpty() || url.endsWith("/"))
			return url;
		return url + "/";
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getServiceUsername() {
		return this.serviceUsername;
	}

	public void setServiceUsername(String serviceUsername) {
		this.serviceUsername = serviceUsername;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public EnumAuthType getAuthType() {
		return authType;
	}

	public void setAuthType(EnumAuthType authType) {
		this.authType = authType;
	}

	public static ServiceConfig.Builder builder() {
		return new ServiceConfig.Builder();
	}

	public static class Builder {
		private String serviceName;
		private String url;
		private String username;
		private String serviceUsername;
		private String apiKey;
		private boolean enabled;

		protected Builder() {
		}

		public ServiceConfig.Builder withServiceName(String serviceName) {
			this.serviceName = serviceName;
			return this;
		}

		public ServiceConfig.Builder withUrl(String url) {
			this.url = url;
			return this;
		}

		public ServiceConfig.Builder withUsername(String username) {
			this.username = username;
			return this;
		}

		public ServiceConfig.Builder withServiceUsername(String serviceUsername) {
			this.serviceUsername = serviceUsername;
			return this;
		}

		public ServiceConfig.Builder withApiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		public ServiceConfig.Builder withEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public ServiceConfig build() {
			return new ServiceConfig(serviceName, url, username, serviceUsername, apiKey, enabled);
		}

	}
}