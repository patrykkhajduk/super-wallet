package io.hyde.wallet.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Configuration
class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.tick(Clock.system(ZoneId.systemDefault()), Duration.of(1, ChronoUnit.NANOS));
    }
}
