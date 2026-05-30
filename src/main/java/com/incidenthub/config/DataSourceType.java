package com.incidenthub.config;

/**
 * Enum representing the type of datasource to route to.
 * MASTER: for write operations (INSERT, UPDATE, DELETE)
 * SLAVE: for read operations (SELECT)
 */
public enum DataSourceType {
  MASTER,
  SLAVE
}
