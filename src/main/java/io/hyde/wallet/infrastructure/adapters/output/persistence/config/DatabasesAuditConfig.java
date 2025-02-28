package io.hyde.wallet.infrastructure.adapters.output.persistence.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Configuration
class DatabasesAuditConfig {

    @Bean
    DateTimeProvider clockBasedDateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }
}
