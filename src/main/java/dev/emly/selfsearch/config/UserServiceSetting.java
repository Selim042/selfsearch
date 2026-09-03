package dev.emly.selfsearch.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_service_settings", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "username", "serviceName" }) })
public class UserServiceSetting {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String username;

	@Column(nullable = false)
	private String serviceName;

	private String url;

	private String serviceUsername;

	private String apiKey;

	@Column(nullable = false)
	private boolean enabled;

	private UserServiceSetting() {

	}

	private UserServiceSetting(Long id, String username, String serviceName, String url, String serviceUsername,
			String apiKey, boolean enabled) {
		super();
		this.id = id;
		this.username = username;
		this.serviceName = serviceName;
		this.url = url;
		this.serviceUsername = serviceUsername;
		this.apiKey = apiKey;
		this.enabled = enabled;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getServiceName() {
		return serviceName;
	}

	public String getUrl() {
		return url;
	}

	public String getServiceUsername() {
		return serviceUsername;
	}

	public String getApiKey() {
		return apiKey;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setServiceUsername(String serviceUsername) {
		this.serviceUsername = serviceUsername;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public static UserServiceSetting.Builder builder() {
		return new UserServiceSetting.Builder();
	}

	public static class Builder {

		private Long id;
		private String username;
		private String serviceName;
		private String url;
		private String serviceUsername;
		private String apiKey;
		private boolean enabled;

		protected Builder() {
		}

		public UserServiceSetting.Builder withId(Long id) {
			this.id = id;
			return this;
		}

		public UserServiceSetting.Builder withUsername(String username) {
			this.username = username;
			return this;
		}

		public UserServiceSetting.Builder withServiceName(String serviceName) {
			this.serviceName = serviceName;
			return this;
		}

		public UserServiceSetting.Builder withUrl(String url) {
			this.url = url;
			return this;
		}

		public UserServiceSetting.Builder withServiceUsername(String serviceUsername) {
			this.serviceUsername = serviceUsername;
			return this;
		}

		public UserServiceSetting.Builder withApiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		public UserServiceSetting.Builder withEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public UserServiceSetting build() {
			return new UserServiceSetting(id, username, serviceName, url, serviceUsername, apiKey, enabled);
		}

	}

}