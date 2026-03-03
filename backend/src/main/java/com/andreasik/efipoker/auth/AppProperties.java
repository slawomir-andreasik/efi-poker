package com.andreasik.efipoker.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String url, CorsProperties cors) {

  public record CorsProperties(String origins) {}
}
