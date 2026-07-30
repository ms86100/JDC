package com.avionics_systems.issue.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.TypeDefinitionRegistry;

/**
 * GraphQL Configuration
 * Configures GraphQL schema loading and custom scalar types
 */
@Configuration
public class GraphQLConfig {

    @Bean
    public GraphQLScalarType uuidScalar() {
        return ExtendedScalars.UUID;
    }

    @Bean
    public GraphQLScalarType dateTimeScalar() {
        return ExtendedScalars.DateTime;
    }

    @Bean
    public GraphQLScalarType jsonScalar() {
        return ExtendedScalars.Json;
    }
}