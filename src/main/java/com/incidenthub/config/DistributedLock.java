package com.incidenthub.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis-based distributed lock for preventing concurrent scheduler execution
 * across multiple application instances.
 * Demonstrates: Distributed systems coordination, Redis SETNX pattern.
 */
@Slf4j
@Component
public class DistributedLock {

  @Nullable
  private final StringRedisTemplate redisTemplate;
  private final String instanceId = UUID.randomUUID().toString();

  public DistributedLock(@Nullable StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * Try to acquire a distributed lock.
   * Uses Redis SET with NX (set if not exists) + EX (expiration).
   *
   * @param lockKey    unique key for the lock
   * @param ttlSeconds lock auto-expiry in seconds (prevents deadlocks)
   * @return true if lock acquired, false if another instance holds it
   */
  public boolean tryLock(String lockKey, long ttlSeconds) {
    if (redisTemplate == null) {
      return true; // Single instance mode when Redis unavailable
    }
    try {
      Boolean acquired = redisTemplate.opsForValue()
          .setIfAbsent(lockKey, instanceId, Duration.ofSeconds(ttlSeconds));
      if (Boolean.TRUE.equals(acquired)) {
        log.debug("Lock acquired: {} by instance {}", lockKey, instanceId);
        return true;
      }
      log.debug("Lock not acquired: {} (held by another instance)", lockKey);
      return false;
    } catch (Exception e) {
      log.warn("Failed to acquire distributed lock (Redis unavailable): {}", e.getMessage());
      // Fallback: allow execution if Redis is down (single instance mode)
      return true;
    }
  }

  /**
   * Release the lock only if current instance holds it (compare-and-delete).
   */
  public void unlock(String lockKey) {
    if (redisTemplate == null)
      return;
    try {
      String holder = redisTemplate.opsForValue().get(lockKey);
      if (instanceId.equals(holder)) {
        redisTemplate.delete(lockKey);
        log.debug("Lock released: {} by instance {}", lockKey, instanceId);
      }
    } catch (Exception e) {
      log.warn("Failed to release distributed lock: {}", e.getMessage());
    }
  }
}
