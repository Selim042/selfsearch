package dev.emly.selfsearch.config;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserServiceSettingRepository extends JpaRepository<UserServiceSetting, Long> {

	List<UserServiceSetting> findByUsername(String username);

	Optional<UserServiceSetting> findByUsernameAndServiceName(String username, String serviceName);
}