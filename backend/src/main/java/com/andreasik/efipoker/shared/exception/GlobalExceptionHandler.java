package com.andreasik.efipoker.shared.exception;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
    log.warn("Resource not found: {}", ex.getMessage());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Resource Not Found");
    problem.setType(URI.create("https://api.efipoker.com/errors/not-found"));
    return withTraceId(problem);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ProblemDetail handleUnauthorized(UnauthorizedException ex) {
    log.warn("Unauthorized access: {}", ex.getMessage());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    problem.setTitle("Unauthorized");
    problem.setType(URI.create("https://api.efipoker.com/errors/unauthorized"));
    return withTraceId(problem);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    log.warn("Validation failed: {}", ex.getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    problem.setTitle("Validation Error");
    problem.setType(URI.create("https://api.efipoker.com/errors/validation"));

    Map<String, String> fieldErrors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
    problem.setProperty("fieldErrors", fieldErrors);

    return withTraceId(problem);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
    log.warn("Constraint violation: {}", ex.getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    problem.setTitle("Validation Error");
    problem.setType(URI.create("https://api.efipoker.com/errors/validation"));

    Map<String, String> fieldErrors = new HashMap<>();
    ex.getConstraintViolations()
        .forEach(v -> fieldErrors.put(v.getPropertyPath().toString(), v.getMessage()));
    problem.setProperty("fieldErrors", fieldErrors);

    return withTraceId(problem);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ProblemDetail handleMissingRequestHeader(MissingRequestHeaderException ex) {
    log.warn("Missing required header: {}", ex.getHeaderName());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setTitle("Missing Required Header");
    problem.setType(URI.create("https://api.efipoker.com/errors/bad-request"));
    return withTraceId(problem);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
    log.warn("Malformed request body: {}", ex.getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
    problem.setTitle("Bad Request");
    problem.setType(URI.create("https://api.efipoker.com/errors/bad-request"));
    return withTraceId(problem);
  }

  @ExceptionHandler(ConflictException.class)
  public ProblemDetail handleConflict(ConflictException ex) {
    log.warn("Conflict: {}", ex.getMessage());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Conflict");
    problem.setType(URI.create("https://api.efipoker.com/errors/conflict"));
    return withTraceId(problem);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ProblemDetail handleIllegalState(IllegalStateException ex) {
    log.warn("Conflict: {}", ex.getMessage());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Conflict");
    problem.setType(URI.create("https://api.efipoker.com/errors/conflict"));
    return withTraceId(problem);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("Bad request: {}", ex.getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setTitle("Bad Request");
    problem.setType(URI.create("https://api.efipoker.com/errors/bad-request"));
    return withTraceId(problem);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
    log.warn(
        "Method not allowed: {} - supported: {}", ex.getMethod(), ex.getSupportedHttpMethods());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.METHOD_NOT_ALLOWED, ex.getMethod() + " method is not supported");
    problem.setTitle("Method Not Allowed");
    problem.setType(URI.create("https://api.efipoker.com/errors/method-not-allowed"));
    problem.setProperty("allowedMethods", ex.getSupportedHttpMethods());
    return withTraceId(problem);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
    log.debug("No resource found: {}", ex.getMessage());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Not Found");
    problem.setType(URI.create("https://api.efipoker.com/errors/not-found"));
    return withTraceId(problem);
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGenericException(Exception ex) {
    log.error("Unexpected error", ex);
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    problem.setTitle("Internal Server Error");
    problem.setType(URI.create("https://api.efipoker.com/errors/internal"));
    return withTraceId(problem);
  }

  private ProblemDetail withTraceId(ProblemDetail problem) {
    String traceId = MDC.get("traceId");
    if (traceId != null && !traceId.isBlank()) {
      problem.setProperty("traceId", traceId);
    }
    return problem;
  }
}
