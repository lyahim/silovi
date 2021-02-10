package hu.lya.silovi;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebFluxSecurity
@Slf4j
public class SecurityConfig {

	private String userName;
	private String password;

	public SecurityConfig(@Value("${auth_username}") final String userName,
			@Value("${auth_password}") final String password) {
		this.userName = userName;
		this.password = password;
	}

	private boolean needAuth() {
		return StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(password);
	}

	@Bean
	public SecurityWebFilterChain securityFilterChain(final ServerHttpSecurity http) {
		if (needAuth()) {
			log.info("Authentication enabled");
			return http.csrf().disable().authorizeExchange().anyExchange().authenticated().and().httpBasic().and()
					.build();
		} else {
			log.info("Authentication disabled");
			return http.authorizeExchange().anyExchange().permitAll().and().build();
		}
	}

	@Bean
	public MapReactiveUserDetailsService userDetailsService() {
		if (needAuth()) {
			UserDetails user = User.builder().username(userName).password("{noop}" + password).roles("USER").build();
			return new MapReactiveUserDetailsService(user);
		}
		return null;
	}
}