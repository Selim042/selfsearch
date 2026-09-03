package dev.emly.selfsearch.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(
				auth -> auth.requestMatchers("/css/**", "/js/**").permitAll().anyRequest().authenticated())
				.oauth2Login(oauth2 -> oauth2
						.userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(this.userAuthoritiesMapper())))
				.logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET")));

		return http.build();
	}

	/**
	 * Maps OIDC provider claims (e.g., "groups" or "roles") to Spring Security
	 * GrantedAuthorities. Maps claim values into standard roles like:
	 * "ROLE_JELLYFIN", "ROLE_BOOKSTACK", etc.
	 */
	@Bean
	public GrantedAuthoritiesMapper userAuthoritiesMapper() {
		return (authorities) -> {
			Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

			authorities.forEach(authority -> {
				mappedAuthorities.add(authority);

				if (authority instanceof OidcUserAuthority oidcUserAuthority) {
					var idToken = oidcUserAuthority.getIdToken();
					var userInfo = oidcUserAuthority.getUserInfo();

					// Check both ID Token and UserInfo for "groups" claim (Keycloak / Authentik
					// standard)
					List<String> groups = null;
					if (idToken != null && idToken.hasClaim("groups")) {
						groups = idToken.getClaimAsStringList("groups");
					} else if (userInfo != null && userInfo.hasClaim("groups")) {
						groups = userInfo.getClaimAsStringList("groups");
					}

					if (groups != null) {
						for (String groupName : groups) {
							// Normalize group names: e.g. "/homelab-jellyfin" or "jellyfin" ->
							// "ROLE_JELLYFIN"
							String cleanRole = groupName.replace("/", "").toUpperCase();
							mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + cleanRole));
						}
					}
				}
			});

			return mappedAuthorities;
		};
	}
}
