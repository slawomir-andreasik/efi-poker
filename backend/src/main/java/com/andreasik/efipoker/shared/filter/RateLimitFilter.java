package com.andreasik.efipoker.shared.filter;

import com.andreasik.efipoker.shared.config.RateLimitConfig;
import com.andreasik.efipoker.shared.exception.ErrorType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

  private final ConcurrentHashMap<String, Deque<Instant>> writeRequestCounts =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Deque<Instant>> readRequestCounts =
      new ConcurrentHashMap<>();

  private final int maxRequests;
  private final int readMaxRequests;
  private final int windowSeconds;

  public RateLimitFilter(RateLimitConfig config) {
    this.maxRequests = config.maxRequests();
    this.readMaxRequests = config.readMaxRequests();
    this.windowSeconds = config.windowSeconds();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    boolean isWrite = WRITE_METHODS.contains(request.getMethod());
    int limit = isWrite ? maxRequests : readMaxRequests;
    ConcurrentHashMap<String, Deque<Instant>> bucket =
        isWrite ? writeRequestCounts : readRequestCounts;

    String clientIp = resolveClientIp(request);
    Instant now = Instant.now();
    Instant windowStart = now.minusSeconds(windowSeconds);

    Deque<Instant> timestamps =
        bucket.computeIfAbsent(clientIp, k -> new ConcurrentLinkedDeque<>());

    // Evict entries older than window
    while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
      timestamps.pollFirst();
    }

    if (timestamps.size() >= limit) {
      log.warn(
          "Rate limit exceeded: ip={}, method={}, count={}",
          clientIp,
          request.getMethod(),
          timestamps.size());
      writeRateLimitResponse(response, limit);
      return;
    }

    timestamps.addLast(now);
    log.debug(
        "Rate limit check: ip={}, method={}, count={}",
        clientIp,
        request.getMethod(),
        timestamps.size());

    filterChain.doFilter(request, response);
  }

  String resolveClientIp(HttpServletRequest request) {
    // Tomcat RemoteIpValve (forward-headers-strategy: NATIVE) resolves X-Forwarded-For
    // from trusted proxies (Docker 172.x, K8s 10.x, localhost) before servlet filters run.
    // remoteAddr is already the real client IP in all deployment scenarios:
    //   - Direct access: remoteAddr = client IP
    //   - Behind proxy (Docker/K8s/nginx): valve resolves from X-Forwarded-For
    //   - Behind Cloudflare+nginx: nginx resolves CF-Connecting-IP, valve resolves X-Forwarded-For
    return request.getRemoteAddr();
  }

  private void writeRateLimitResponse(HttpServletResponse response, int limit) throws IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setIntHeader("Retry-After", windowSeconds);
    response.setIntHeader("X-RateLimit-Limit", limit);
    response.setIntHeader("X-RateLimit-Remaining", 0);
    response
        .getWriter()
        .write(
            "{\"type\":\""
                + ErrorType.RATE_LIMIT_EXCEEDED.uri()
                + "\","
                + "\"title\":\"Too Many Requests\","
                + "\"status\":429,"
                + "\"detail\":\"Rate limit exceeded. Try again later.\"}");
  }

  @Scheduled(fixedRate = 60_000)
  void cleanupAllBuckets() {
    cleanupStaleEntries(writeRequestCounts);
    cleanupStaleEntries(readRequestCounts);
  }

  private void cleanupStaleEntries(ConcurrentHashMap<String, Deque<Instant>> bucket) {
    Instant cutoff = Instant.now().minusSeconds(windowSeconds);
    bucket
        .entrySet()
        .removeIf(
            entry -> {
              Deque<Instant> deque = entry.getValue();
              while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.pollFirst();
              }
              return deque.isEmpty();
            });
  }
}
