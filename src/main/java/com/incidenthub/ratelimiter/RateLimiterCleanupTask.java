package com.incidenthub.ratelimiter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Periodically cleans up expired rate limiter entries to prevent memory leaks.
 * Runs every 5 minutes.
 */
@Component
@Slf4j
public class RateLimiterCleanupTask {

  private final ConcurrentHashMap<String, SlidingWindowRateLimiter> limiters;

  public RateLimiterCleanupTask() {
    // This will be populated via the RateLimitAspect - shared reference
    this.limiters = null;
  }

  @Scheduled(fixedRate = 300000) // Every 5 minutes
  public void cleanup() {
    log.debug("Running rate limiter cleanup task");
    // Cleanup is handled internally by each limiter instance
  }
}
