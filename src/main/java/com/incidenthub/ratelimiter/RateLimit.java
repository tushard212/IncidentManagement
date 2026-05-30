package com.incidenthub.ratelimiter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for rate limiting API endpoints.
 * Uses a sliding window algorithm per client IP.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

  /**
   * Maximum number of requests allowed within the time window.
   */
  int maxRequests() default 100;

  /**
   * Time window in seconds.
   */
  int windowSeconds() default 60;

  /**
   * Key type for rate limiting - IP, USER, or GLOBAL.
   */
  KeyType keyType() default KeyType.IP;

  enum KeyType {
    IP,
    USER,
    GLOBAL
  }
}
