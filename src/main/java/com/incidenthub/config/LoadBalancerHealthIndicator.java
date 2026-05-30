package com.incidenthub.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Custom health indicator for load balancer health checks.
 * Reports application readiness for traffic routing.
 */
@Component("loadBalancerHealth")
public class LoadBalancerHealthIndicator implements HealthIndicator {

  private final DataSource dataSource;
  private volatile boolean accepting = true;

  public LoadBalancerHealthIndicator(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Health health() {
    if (!accepting) {
      return Health.outOfService()
          .withDetail("reason", "Instance marked for drain")
          .build();
    }

    try (Connection conn = dataSource.getConnection()) {
      if (conn.isValid(2)) {
        return Health.up()
            .withDetail("database", "reachable")
            .withDetail("accepting_traffic", true)
            .build();
      }
    } catch (Exception e) {
      return Health.down()
          .withDetail("database", "unreachable")
          .withDetail("error", e.getMessage())
          .build();
    }

    return Health.down().withDetail("database", "connection invalid").build();
  }

  /**
   * Gracefully drain this instance - stop accepting new traffic
   * while existing requests complete. Used during rolling deployments.
   */
  public void drain() {
    this.accepting = false;
  }

  public void resume() {
    this.accepting = true;
  }
}
