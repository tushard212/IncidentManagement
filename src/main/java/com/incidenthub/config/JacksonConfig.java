package com.incidenthub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Configures Jackson to serialize Long values as Strings in JSON responses.
 * This prevents JavaScript precision loss for Snowflake IDs (64-bit) which
 * exceed
 * Number.MAX_SAFE_INTEGER (2^53 - 1).
 */
@Configuration
public class JacksonConfig {

  @Bean
  public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
    ObjectMapper mapper = builder.build();
    SimpleModule module = new SimpleModule();
    module.addSerializer(Long.class, ToStringSerializer.instance);
    module.addSerializer(Long.TYPE, ToStringSerializer.instance);
    mapper.registerModule(module);
    return mapper;
  }
}
