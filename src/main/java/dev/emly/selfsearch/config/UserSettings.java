package dev.emly.selfsearch.config;

import java.util.HashMap;
import java.util.Map;

public class UserSettings {
	private Map<String, ServiceConfig> services = new HashMap<>();

	public ServiceConfig getConfig(String serviceName) {
		return services.getOrDefault(serviceName, new ServiceConfig(serviceName, "", "", "", "", false));
	}

	public void setServices(Map<String, ServiceConfig> configMap) {
		this.services = configMap;
	}

	public Map<String, ServiceConfig> getServices() {
		return services;
	}
}