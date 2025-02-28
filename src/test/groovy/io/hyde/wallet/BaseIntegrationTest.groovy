package io.hyde.wallet

import io.hyde.wallet.utils.TestHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
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
@AutoConfigureWebTestClient
abstract class BaseIntegrationTest extends Specification {

    public static final String WALLET_COMMANDS_TOPIC = "wallet-commands"
    public static final String WALLET_COMMANDS_DEAD_LETTER_TOPIC = WALLET_COMMANDS_TOPIC + "-dlt"
    public static final String WALLET_EVENTS_TOPIC = "wallet-events"

    @Shared
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0.0")

    @Shared
    static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.1"))


    @LocalServerPort
    int port

    @Autowired
    protected TestHelper testHelper

    @Autowired
    protected WebTestClient webTestClient

    def setupSpec() {
        Stream.of(mongoDBContainer, kafkaContainer)
                .parallel()
                .forEach(GenericContainer::start)
    }

    def setup() {
        testHelper.clearData()
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
        registry.add("wallets.limit-per-owner", () -> 3)
        registry.add("jobs.send-executed-commands.cron", () -> "0 0 0 * * ?")
        registry.add("jobs.send-executed-commands.events-creation-date-delay", () -> Duration.ofMillis(1))
        registry.add("jobs.process-missing-executed-commands.cron", () -> "0 0 0 * * ?")
        registry.add("jobs.process-missing-executed-commands.wallets-last-modified-date-delay", () -> Duration.ofMillis(1))
    }
}
