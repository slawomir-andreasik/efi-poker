package com.andreasik.efipoker.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

@Configuration
@ConditionalOnProperty(name = "app.auth0.enabled", havingValue = "true")
@RequiredArgsConstructor
public class Auth0ClientConfig {

  private final Auth0Properties auth0Properties;

  @Bean
  public ClientRegistrationRepository clientRegistrationRepository() {
    ClientRegistration auth0 =
        ClientRegistration.withRegistrationId("auth0")
            .clientId(auth0Properties.clientId())
            .clientSecret(auth0Properties.clientSecret())
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/api/v1/auth/oauth2/callback")
            .scope("openid", "profile", "email")
            .authorizationUri("https://" + auth0Properties.domain() + "/authorize")
            .tokenUri("https://" + auth0Properties.domain() + "/oauth/token")
            .userInfoUri("https://" + auth0Properties.domain() + "/userinfo")
            .userNameAttributeName(IdTokenClaimNames.SUB)
            .jwkSetUri("https://" + auth0Properties.domain() + "/.well-known/jwks.json")
            .clientName("Auth0")
            .build();

    return new InMemoryClientRegistrationRepository(auth0);
  }
}
