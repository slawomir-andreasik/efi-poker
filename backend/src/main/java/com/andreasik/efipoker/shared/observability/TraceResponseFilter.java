package com.andreasik.efipoker.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class TraceResponseFilter extends OncePerRequestFilter {

  static final String TRACE_ID_HEADER = "X-Trace-Id";
  static final String MDC_TRACE_ID = "traceId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = MDC.get(MDC_TRACE_ID);
    if (traceId != null && !traceId.isBlank()) {
      response.setHeader(TRACE_ID_HEADER, traceId);
    }
    filterChain.doFilter(request, response);
  }
}
