package com.incidenthub.ratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AOP Aspect that intercepts methods annotated with @RateLimit.
 * Extracts client key (IP/User/Global) and enforces rate limiting
 * using the SlidingWindowRateLimiter.
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

  private final ConcurrentHashMap<String, SlidingWindowRateLimiter> limiters = new ConcurrentHashMap<>();

  @Around("@annotation(rateLimit)")
  public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
    String key = resolveKey(joinPoint, rateLimit);
    String limiterKey = getLimiterKey(joinPoint);

    SlidingWindowRateLimiter limiter = limiters.computeIfAbsent(limiterKey,
        k -> new SlidingWindowRateLimiter(rateLimit.maxRequests(), rateLimit.windowSeconds()));

    // Set rate limit headers
    setRateLimitHeaders(limiter, key);

    if (!limiter.tryAcquire(key)) {
      log.warn("Rate limit exceeded for key: {} on endpoint: {}", key, limiterKey);
      throw new RateLimitExceededException(
          "Rate limit exceeded. Max " + rateLimit.maxRequests() + " requests per " + rateLimit.windowSeconds() + "s");
    }

    return joinPoint.proceed();
  }

  private String resolveKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
    return switch (rateLimit.keyType()) {
      case IP -> getClientIp();
      case USER -> getCurrentUsername();
      case GLOBAL -> "GLOBAL";
    };
  }

  private String getLimiterKey(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    return method.getDeclaringClass().getSimpleName() + "." + method.getName();
  }

  private String getClientIp() {
    ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs == null)
      return "unknown";
    HttpServletRequest request = attrs.getRequest();

    // Check X-Forwarded-For header (behind load balancer)
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }

    return request.getRemoteAddr();
  }

  private String getCurrentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()) {
      return auth.getName();
    }
    return getClientIp(); // Fallback to IP for unauthenticated
  }

  private void setRateLimitHeaders(SlidingWindowRateLimiter limiter, String key) {
    ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs == null)
      return;
    HttpServletResponse response = attrs.getResponse();
    if (response == null)
      return;

    int remaining = limiter.getRemainingRequests(key);
    long resetTime = limiter.getResetTimeMillis(key);

    response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
    response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime / 1000));
  }
}
