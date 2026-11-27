package com.gregomebije.wallet.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import io.jsonwebtoken.Claims;

import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Component
public class JwtService {
	static final long EXPIRATIONTIME = 86400000; // 1 day in ms
	static final String PREFIX = "Bearer "; // Note the space after Bearer

	static final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

	public String getToken(String username, Collection<? extends GrantedAuthority> authorities) {
		String roles = authorities.stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.joining(","));

		return Jwts.builder()
				.setSubject(username)
				.claim("roles", roles) // Adds e.g., "ROLE_USER" to the token payload
				.setExpiration(new Date(System.currentTimeMillis() + EXPIRATIONTIME))
				.signWith(key)
				.compact();
	}

    // Get a token from request Authorization header,
	// verify the token, and get username
	public String getAuthUser(HttpServletRequest request) {
		String token = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (token != null) {
			String user = Jwts.parserBuilder().setSigningKey(key).build()
					.parseClaimsJws(token.replace(PREFIX, ""))
					.getBody().getSubject();
			if (user != null)
				return user;
		}
		return null;
	}

	public Collection<? extends GrantedAuthority> getAuthRoles(HttpServletRequest request) {
		String token = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (token != null) {
			String rolesString = Jwts.parserBuilder().setSigningKey(key).build()
					.parseClaimsJws(token.replace(PREFIX, ""))
					.getBody().get("roles", String.class);
			
			if (rolesString != null && !rolesString.isEmpty()) {
				return java.util.Arrays.stream(rolesString.split(","))
						.map(SimpleGrantedAuthority::new)
						.collect(Collectors.toList());
			}
		}
		return java.util.Collections.emptyList();
	}
}
