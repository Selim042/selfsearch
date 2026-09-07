package dev.emly.selfsearch;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

	private final JdbcTemplate jdbcTemplate;

	public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@Transactional
	public void run(String... args) {
		try {
			// 1. Ensure schema version tracking table exists
			jdbcTemplate.execute("""
					    CREATE TABLE IF NOT EXISTS schema_version (
					        version INT PRIMARY KEY,
					        description VARCHAR(255) NOT NULL,
					        applied_at TIMESTAMP NOT NULL
					    )
					""");

			// 2. Migration V1: Expand api_key column to VARCHAR(512)
			applyMigration(1, "Expand user_service_settings api_key column to VARCHAR(512)", () -> {
				jdbcTemplate.execute("ALTER TABLE user_service_settings ALTER COLUMN api_key SET DATA TYPE VARCHAR(512)");
			});

		} catch (Exception e) {
			System.err.println("[Migration] Failed to execute database migrations: " + e.getMessage());
		}
	}

	private void applyMigration(int version, String description, Runnable migrationTask) {
		String checkVersionSql = "SELECT COUNT(*) FROM schema_version WHERE version = ?";
		Integer count = jdbcTemplate.queryForObject(checkVersionSql, Integer.class, version);

		if (count == null || count == 0) {
			System.out.println(String.format("[Migration] Executing V%d: %s...", version, description));

			migrationTask.run();

			String recordVersionSql = "INSERT INTO schema_version (version, description, applied_at) VALUES (?, ?, ?)";
			jdbcTemplate.update(recordVersionSql, version, description, Timestamp.from(Instant.now()));

			System.out.println(String.format("[Migration] Successfully applied V%d.", version));
		} else {
			System.out.println(String.format("[Migration] Schema V%d is already applied. Skipping.", version));
		}
	}
}