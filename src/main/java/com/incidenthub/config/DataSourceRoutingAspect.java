package com.incidenthub.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AOP Aspect that intercepts @Transactional methods and routes
 * to the appropriate datasource:
 * - readOnly=true → SLAVE (read replica)
 * - readOnly=false or write operations → MASTER
 *
 * Order(-1) ensures this runs BEFORE the transaction manager opens a
 * connection.
 */
@Aspect
@Component
@Order(-1)
@Slf4j
public class DataSourceRoutingAspect {

  @Around("@annotation(transactional)")
  public Object route(ProceedingJoinPoint joinPoint, Transactional transactional) throws Throwable {
    DataSourceType type = transactional.readOnly() ? DataSourceType.SLAVE : DataSourceType.MASTER;
    DataSourceContextHolder.setDataSourceType(type);

    log.debug("Routing to {} datasource for method: {}", type,
        joinPoint.getSignature().toShortString());

    try {
      return joinPoint.proceed();
    } finally {
      DataSourceContextHolder.clear();
    }
  }
}
