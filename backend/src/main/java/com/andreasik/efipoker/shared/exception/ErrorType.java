package com.andreasik.efipoker.shared.exception;

import java.net.URI;

public enum ErrorType {
  NOT_FOUND("not-found"),
  UNAUTHORIZED("unauthorized"),
  VALIDATION("validation"),
  BAD_REQUEST("bad-request"),
  CONFLICT("conflict"),
  METHOD_NOT_ALLOWED("method-not-allowed"),
  RATE_LIMIT_EXCEEDED("rate-limit-exceeded"),
  INTERNAL("internal");

  private static final String BASE_URI = "https://efipoker.com/errors";
  private final URI uri;

  ErrorType(String slug) {
    this.uri = URI.create(BASE_URI + "/" + slug);
  }

  public URI uri() {
    return uri;
  }
}
