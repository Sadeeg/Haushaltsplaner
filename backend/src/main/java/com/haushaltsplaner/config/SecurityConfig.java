package com.haushaltsplaner.config;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haushaltsplaner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;

    @Autowired
    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public JwtDecoder jwtDecoder() throws Exception {
        JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(
                new URL("https://cloud.deeg-mail.de/apps/oidc/jwks")
        );

        ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();

        // at+jwt Typ explizit erlauben
        processor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(
                new JOSEObjectType("at+jwt"),
                new JOSEObjectType("JWT"),
                JOSEObjectType.JWT,
                null
        ));

        // RS256 und HS256 – beide unterstützt laut Nextcloud OIDC
        processor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource)
        );

        NimbusJwtDecoder decoder = new NimbusJwtDecoder(processor);

        // Issuer validieren
        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer("https://cloud.deeg-mail.de")
        );

        return decoder;
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

        return userRequest -> {
            OAuth2User user = delegate.loadUser(userRequest);
            
            if (!"nextcloud".equals(userRequest.getClientRegistration().getRegistrationId())) {
                return user;
            }

            // Extract user info from Nextcloud OIDC
            String nextcloudId = user.getName();
            String username = user.getAttribute("preferred_username");
            String email = user.getAttribute("email");
            String displayName = user.getAttribute("name");

            if (nextcloudId == null || username == null) {
                OAuth2Error error = new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_REQUEST,
                    "Missing required user attributes from Nextcloud",
                    null
                );
                throw new OAuth2AuthenticationException(error);
            }

            // Create or update user in database
            userService.createOrUpdateFromOAuth(username, email, nextcloudId, displayName);

            return user;
        };
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            
            String nextcloudId = oauth2User.getName();
            String username = oauth2User.getAttribute("preferred_username");
            String email = oauth2User.getAttribute("email");
            String displayName = oauth2User.getAttribute("name");

            try {
                var userDto = userService.createOrUpdateFromOAuth(username, email, nextcloudId, displayName);
                
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                
                ObjectMapper mapper = new ObjectMapper();
                var result = new java.util.HashMap<String, Object>();
                result.put("user", userDto);
                result.put("message", "Login successful");
                
                response.getWriter().write(mapper.writeValueAsString(result));
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\": \"Failed to process OAuth login\"}");
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder)
            throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health", "/api/auth/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/login/oauth2/code/**", "/api/oauth2/authorization/**").permitAll()
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/api/oauth2/authorization/nextcloud")
                .authorizationEndpoint(authorization -> authorization
                    .baseUri("/api/oauth2/authorization")
                )
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/api/login/oauth2/code/*")
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oauth2UserService())
                )
                .successHandler(authenticationSuccessHandler())
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder))
            );

        return http.build();
    }
}
