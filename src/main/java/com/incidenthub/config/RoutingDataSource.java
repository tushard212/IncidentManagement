package com.incidenthub.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Custom routing datasource that routes read queries to slave
 * and write queries to master.
 *
 * Routing logic:
 * - @Transactional(readOnly = true) → routes to SLAVE
 * - @Transactional or no annotation → routes to MASTER
 *
 * This is the core of Master-Slave DB architecture:
 * - MASTER handles writes (INSERT/UPDATE/DELETE)
 * - SLAVE(s) handle reads (SELECT) - can scale horizontally
 * - Reduces load on master, improves read throughput
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

  @Override
  protected Object determineCurrentLookupKey() {
    DataSourceType type = DataSourceContextHolder.getDataSourceType();
    if (type == null) {
      return DataSourceType.MASTER; // Default to master
    }
    return type;
  }
}
