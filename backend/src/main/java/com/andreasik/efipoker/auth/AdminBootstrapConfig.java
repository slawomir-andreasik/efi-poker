package com.andreasik.efipoker.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminBootstrapConfig {

  private final AdminProperties adminProperties;

  @Bean
  public CommandLineRunner bootstrapAdmin(
      UserRepository userRepository, PasswordEncoder passwordEncoder) {
    return args -> {
      if (userRepository.findByUsername(adminProperties.username()).isPresent()) {
        log.info("Admin user already exists: {}", adminProperties.username());
        return;
      }

      UserEntity admin =
          UserEntity.builder()
              .username(adminProperties.username())
              .passwordHash(passwordEncoder.encode(adminProperties.password()))
              .authProvider(AuthProvider.LOCAL.name())
              .role(UserRole.ADMIN.name())
              .build();

      userRepository.save(admin);
      log.info("Admin user created: {}", adminProperties.username());
    };
  }
}
