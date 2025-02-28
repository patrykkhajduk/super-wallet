package io.hyde.wallet;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableReactiveMongoRepositories
@EnableReactiveMongoAuditing
@EnableKafka
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT60S")
public class SuperWalletApplication {

    public static void main(String... args) {
        SpringApplication.run(SuperWalletApplication.class, args);
    }
}