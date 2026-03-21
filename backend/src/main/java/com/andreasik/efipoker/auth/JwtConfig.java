package com.andreasik.efipoker.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {

  private final JwtProperties jwtProperties;

  @Bean
  public JwtDecoder jwtDecoder() {
    SecretKeySpec key =
        new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA512");
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS512).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtService.ISSUER));
    return decoder;
  }

  @Bean
  public JwtEncoder jwtEncoder() {
    SecretKeySpec key =
        new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA512");
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          String tokenType = jwt.getClaimAsString(JwtService.CLAIM_TYPE);

          if (JwtService.TOKEN_TYPE_GUEST.equals(tokenType)) {
            Boolean isAdmin = jwt.getClaim(JwtService.CLAIM_ADMIN);
            if (Boolean.TRUE.equals(isAdmin)) {
              return List.of(
                  new SimpleGrantedAuthority("ROLE_GUEST"),
                  new SimpleGrantedAuthority("ROLE_PROJECT_ADMIN"));
            }
            return List.of(new SimpleGrantedAuthority("ROLE_GUEST"));
          }

          String role = jwt.getClaimAsString(JwtService.CLAIM_ROLE);
          if (role != null) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
          }
          return List.of();
        });
    return converter;
  }
}
