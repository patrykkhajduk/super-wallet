package io.hyde.wallet

import io.hyde.wallet.utils.PerformanceTestHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.util.stream.Stream

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(classes = SuperWalletApplication.class, webEnvironment = RANDOM_PORT)
abstract class BasePerformanceTest extends Specification {

    public static final String WALLET_COMMANDS_TOPIC = "wallet-commands"
    public static final String WALLET_EVENTS_TOPIC = "wallet-events"

    @Shared
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0.0")

    @Shared
    static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.1"))

    @Autowired
    protected PerformanceTestHelper performanceTestHelper

    def setupSpec() {
        Stream.of(mongoDBContainer, kafkaContainer)
                .parallel()
                .forEach(GenericContainer::start)
    }

    def setup() {
        performanceTestHelper.clearData()
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        //Some of those properties are static so they could be setup in annotation
        //yet I like to keep them all in one place for better readability
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getConnectionString)
        registry.add("spring.data.mongodb.database", () -> "test")
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers)
        registry.add("spring.kafka.consumer.group-id", () -> "test-consumer-group")
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest")
        registry.add("spring.kafka.consumer.topics.wallet-commands.name", () -> WALLET_COMMANDS_TOPIC)
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*")
        registry.add("spring.kafka.producer.topics.wallet-events.name", () -> WALLET_EVENTS_TOPIC)
        registry.add("spring.kafka.producer.topics.wallet-events.retry-count", () -> 0)
        registry.add("wallets.limit-per-owner", () -> 3)
        registry.add("jobs.process-not-completed-wallet-processes-steps.cron", () -> "0 0 0 * * ?")
        registry.add("jobs.process-not-completed-wallet-processes-steps.processes-creation-date-delay", () -> Duration.ofMillis(1))
    }
}
