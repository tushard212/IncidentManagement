package com.incidenthub.config;

/**
 * ThreadLocal holder for the current datasource routing key.
 * Used by RoutingDataSource to determine master/slave routing.
 *
 * Read-only transactions are automatically routed to SLAVE.
 * Write transactions go to MASTER.
 */
public class DataSourceContextHolder {

  private static final ThreadLocal<DataSourceType> contextHolder = new ThreadLocal<>();

  public static void setDataSourceType(DataSourceType type) {
    contextHolder.set(type);
  }

  public static DataSourceType getDataSourceType() {
    return contextHolder.get();
  }

  public static void clear() {
    contextHolder.remove();
  }
}
