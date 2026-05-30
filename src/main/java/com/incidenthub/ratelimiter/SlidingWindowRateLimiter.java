package com.incidenthub.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sliding Window Rate Limiter implementation.
 * Uses a combination of fixed window counters and sliding window log
 * for efficient and accurate rate limiting.
 *
 * Algorithm:
 * - Maintains a deque of request timestamps per key
 * - On each request, removes expired timestamps outside the window
 * - If remaining count < maxRequests, allows the request
 * - Thread-safe using ConcurrentHashMap + synchronized blocks per key
 */
public class SlidingWindowRateLimiter {

  private final int maxRequests;
  private final long windowMillis;
  private final ConcurrentHashMap<String, SlidingWindow> windows = new ConcurrentHashMap<>();

  public SlidingWindowRateLimiter(int maxRequests, int windowSeconds) {
    this.maxRequests = maxRequests;
    this.windowMillis = windowSeconds * 1000L;
  }

  /**
   * Attempts to acquire a permit for the given key.
   *
   * @param key the rate limit key (IP, userId, etc.)
   * @return true if request is allowed, false if rate limited
   */
  public boolean tryAcquire(String key) {
    SlidingWindow window = windows.computeIfAbsent(key, k -> new SlidingWindow());
    return window.tryAcquire();
  }

  /**
   * Returns remaining requests for the given key.
   */
  public int getRemainingRequests(String key) {
    SlidingWindow window = windows.get(key);
    if (window == null)
      return maxRequests;
    return window.getRemaining();
  }

  /**
   * Returns when the window resets (epoch millis) for the given key.
   */
  public long getResetTimeMillis(String key) {
    SlidingWindow window = windows.get(key);
    if (window == null)
      return System.currentTimeMillis() + windowMillis;
    return window.getResetTime();
  }

  /**
   * Cleanup expired entries to prevent memory leaks.
   * Should be called periodically.
   */
  public void cleanup() {
    long now = System.currentTimeMillis();
    windows.entrySet().removeIf(entry -> {
      SlidingWindow w = entry.getValue();
      return w.getLastAccessTime() + windowMillis * 2 < now;
    });
  }

  private class SlidingWindow {
    private final ConcurrentLinkedDeque<Long> timestamps = new ConcurrentLinkedDeque<>();
    private final AtomicInteger count = new AtomicInteger(0);
    private volatile long lastAccessTime = System.currentTimeMillis();

    synchronized boolean tryAcquire() {
      long now = System.currentTimeMillis();
      lastAccessTime = now;
      long windowStart = now - windowMillis;

      // Remove expired timestamps
      while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
        timestamps.pollFirst();
        count.decrementAndGet();
      }

      // Check if under limit
      if (count.get() < maxRequests) {
        timestamps.addLast(now);
        count.incrementAndGet();
        return true;
      }

      return false;
    }

    synchronized int getRemaining() {
      long now = System.currentTimeMillis();
      long windowStart = now - windowMillis;

      while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
        timestamps.pollFirst();
        count.decrementAndGet();
      }

      return Math.max(0, maxRequests - count.get());
    }

    long getResetTime() {
      Long first = timestamps.peekFirst();
      if (first == null)
        return System.currentTimeMillis() + windowMillis;
      return first + windowMillis;
    }

    long getLastAccessTime() {
      return lastAccessTime;
    }
  }
}
