package io.hyde.wallet.infrastructure.adapters.output.persistence.config;

import com.mongodb.client.MongoClient;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SchedulerConfig {

    @Bean
    LockProvider lockProvider(MongoProperties mongoProperties, MongoClient mongoClient) {
        return new MongoLockProvider(mongoClient.getDatabase(mongoProperties.getDatabase()));
    }
}
