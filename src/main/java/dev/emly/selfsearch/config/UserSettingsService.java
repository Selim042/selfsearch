package dev.emly.selfsearch.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.emly.selfsearch.service.SearchService;

@Service
public class UserSettingsService {

	private final UserServiceSettingRepository repository;
	private final List<SearchService> availableServices;

	public UserSettingsService(UserServiceSettingRepository repository, List<SearchService> availableServices) {
		this.repository = repository;
		this.availableServices = availableServices;
	}

	public UserSettings getUserSettings(String username) {
		List<UserServiceSetting> savedSettings = repository.findByUsername(username);

		Map<String, UserServiceSetting> savedMap = savedSettings.stream()
				.collect(Collectors.toMap(UserServiceSetting::getServiceName, s -> s));

		Map<String, ServiceConfig> configMap = new HashMap<>();

		// Fill in missing service defaults for services not yet configured by the user
		for (SearchService service : availableServices) {
			UserServiceSetting saved = savedMap.get(service.getServiceName());

			ServiceConfig config = ServiceConfig.builder().withServiceName(service.getServiceName())
					.withUrl(saved != null ? saved.getUrl() : "")
					.withServiceUsername(saved != null ? saved.getServiceUsername() : "")
					.withApiKey(saved != null ? saved.getApiKey() : "").withEnabled(saved != null && saved.isEnabled())
					.build();
			config.setAuthType(service.getAuthType());

			configMap.put(service.getServiceName(), config);
		}

		UserSettings userSettings = new UserSettings();
		userSettings.setServices(configMap);
		return userSettings;
	}

	@Transactional
	public void saveUserSettings(String username, UserSettings userSettings) {
		if (userSettings.getServices() == null)
			return;

		userSettings.getServices().forEach((serviceName, config) -> {
			UserServiceSetting setting = repository.findByUsernameAndServiceName(username, serviceName).orElseGet(
					() -> UserServiceSetting.builder().withUsername(username).withServiceName(serviceName).build());

			setting.setUrl(config.getUrl());
			setting.setEnabled(config.isEnabled());
			setting.setServiceUsername(config.getServiceUsername());

			// Retain existing API key if form field was left blank/empty
			if (config.getApiKey() != null && !config.getApiKey().isBlank())
				setting.setApiKey(config.getApiKey());
			// If config.getApiKey() is blank, setting.getApiKey() remains unchanged

			repository.save(setting);
		});
	}
}