package com.transport.erp.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prevents Jackson from failing on Hibernate lazy proxies
 * (ByteBuddyInterceptor / hibernateLazyInitializer) during API responses.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        // Do not trigger lazy loads during JSON serialization
        module.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        // Emit id for unloaded lazy associations instead of failing
        module.enable(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS);
        return module;
    }
}
