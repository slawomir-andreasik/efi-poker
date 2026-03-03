package com.andreasik.efipoker.shared.exception;

public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }

  public ResourceNotFoundException(String resource, Object id) {
    super(resource + " not found");
  }
}
