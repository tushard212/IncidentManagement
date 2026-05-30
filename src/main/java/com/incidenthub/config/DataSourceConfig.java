package com.incidenthub.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configures Master-Slave database routing.
 *
 * Architecture:
 * ┌─────────────────────────────────────────────────────┐
 * │ Application │
 * │ │ │
 * │ RoutingDataSource │
 * │ / \ │
 * │ MASTER SLAVE │
 * │ (Read+Write) (Read Only) │
 * │ │ │ │
 * │ Primary DB Replica DB │
 * └─────────────────────────────────────────────────────┘
 *
 * In dev mode (H2), both master and slave point to same in-memory DB.
 * In production, slave points to read replicas.
 */
@Configuration
public class DataSourceConfig {

  @Value("${spring.datasource.url}")
  private String masterUrl;

  @Value("${spring.datasource.username}")
  private String masterUsername;

  @Value("${spring.datasource.password}")
  private String masterPassword;

  @Value("${spring.datasource.driver-class-name}")
  private String driverClassName;

  @Value("${app.datasource.slave.url:${spring.datasource.url}}")
  private String slaveUrl;

  @Value("${app.datasource.slave.username:${spring.datasource.username}}")
  private String slaveUsername;

  @Value("${app.datasource.slave.password:${spring.datasource.password}}")
  private String slavePassword;

  @Bean
  @Primary
  public DataSource dataSource() {
    RoutingDataSource routingDataSource = new RoutingDataSource();

    DataSource masterDataSource = createDataSource(masterUrl, masterUsername, masterPassword, "master-pool");
    DataSource slaveDataSource = createDataSource(slaveUrl, slaveUsername, slavePassword, "slave-pool");

    Map<Object, Object> targetDataSources = new HashMap<>();
    targetDataSources.put(DataSourceType.MASTER, masterDataSource);
    targetDataSources.put(DataSourceType.SLAVE, slaveDataSource);

    routingDataSource.setTargetDataSources(targetDataSources);
    routingDataSource.setDefaultTargetDataSource(masterDataSource);
    routingDataSource.afterPropertiesSet();

    return routingDataSource;
  }

  private DataSource createDataSource(String url, String username, String password, String poolName) {
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl(url);
    ds.setUsername(username);
    ds.setPassword(password);
    ds.setDriverClassName(driverClassName);
    ds.setPoolName(poolName);
    ds.setMaximumPoolSize(10);
    ds.setMinimumIdle(2);
    ds.setConnectionTimeout(30000);
    ds.setIdleTimeout(600000);
    ds.setMaxLifetime(1800000);
    return ds;
  }
}
