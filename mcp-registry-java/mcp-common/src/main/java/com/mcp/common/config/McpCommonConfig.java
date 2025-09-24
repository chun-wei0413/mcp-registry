package com.mcp.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP common configuration
 */
@Configuration
public class McpCommonConfig {

    /**
     * Jackson ObjectMapper configuration
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register Java Time module
        mapper.registerModule(new JavaTimeModule());

        // Use snake_case naming strategy
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        // Ignore unknown properties
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }
}