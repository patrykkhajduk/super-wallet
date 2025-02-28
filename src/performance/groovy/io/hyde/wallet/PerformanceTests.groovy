package io.hyde.wallet

import io.hyde.wallet.domain.model.Wallet
import io.hyde.wallet.domain.model.command.DepositFundsCommand
import io.hyde.wallet.domain.model.command.WalletCommand
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.DepositFundsCommandEvent
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.WalletCommandEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Flux
import spock.util.concurrent.PollingConditions

import java.util.stream.IntStream

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(classes = SuperWalletApplication.class, webEnvironment = RANDOM_PORT)
class PerformanceTests extends BasePerformanceTest {

    //Adding logger to print test progress on WARN level
    private static final Logger log = LoggerFactory.getLogger(PerformanceTests)
    private static final String BTC = "BTC"

    def "should process #eventsCount wallet events having #initialEntitiesCount wallets and commands"() {
        given:
        BigDecimal amount = BigDecimal.ONE

        and:
        log.warn("Init token, wallets and execute initial commands")
        performanceTestHelper.initToken(BTC)
        performanceTestHelper.initWallets(initialEntitiesCount).subscribe()

        Wallet wallet = performanceTestHelper.initWallet()
        Flux<WalletCommand> commandsFlux = Flux.fromStream(IntStream.rangeClosed(1, initialEntitiesCount).boxed())
                .map(i -> new DepositFundsCommand(
                        UUID.randomUUID().toString(), wallet.getId(), BTC, amount) as WalletCommand)
        performanceTestHelper.executeAndStoreSendCommands(wallet, commandsFlux).subscribe()

        new PollingConditions(timeout: initialEntitiesCount / 100, factor: 1.1).eventually {
            performanceTestHelper.getWalletsCount() == initialEntitiesCount + 1
            performanceTestHelper.getExecutedCommandsCount() == initialEntitiesCount
            performanceTestHelper.getWallet(wallet.getId()).getFunds()[BTC].getAvailable() == initialEntitiesCount * amount
        }

        and:
        log.warn("Warm up kafka")
        performanceTestHelper.sendWalletCommandEvent(
                new DepositFundsCommandEvent(UUID.randomUUID().toString(), wallet.getId(), BTC, amount))
        new PollingConditions(timeout: 5).eventually {
            performanceTestHelper.getWallet(wallet.getId())
                    .getFunds()[BTC]
                    .getAvailable() == (initialEntitiesCount + 1) * amount
        }

        when:
        log.warn("Sending events")
        Flux<WalletCommandEvent> events = Flux.fromStream(IntStream.rangeClosed(1, eventsCount).boxed())
                .map({ new DepositFundsCommandEvent(UUID.randomUUID().toString(), wallet.getId(), BTC, amount) })
        performanceTestHelper.sendWalletCommandEvents(events).subscribe()
        long start = System.currentTimeMillis()

        then:
        log.warn("Wait for all events to be processed")
        new PollingConditions(timeout: eventsCount / 10).eventually {
            performanceTestHelper.getWallet(wallet.getId())
                    .getFunds()[BTC]
                    .getAvailable() == (initialEntitiesCount + eventsCount + 1) * amount
        }
        and:
        log.warn("Check results")
        performanceTestHelper.getWalletsCount() == initialEntitiesCount + 1
        performanceTestHelper.getExecutedCommandsCount() == initialEntitiesCount + eventsCount + 1

        and:
        log.warn("Print results")
        long elapsed = System.currentTimeMillis() - start
        println("Wallets count: ${performanceTestHelper.getWalletsCount()}")
        println("Executed commands count: ${performanceTestHelper.getWalletsCount()}")
        println("Wallet $BTC funds: ${performanceTestHelper.getWallet(wallet.getId()).getFunds()[BTC].getAvailable()}")
        println("Total execution time: ${elapsed / 1000} [s]")
        println("Average execution time per command: ${elapsed / eventsCount} [ms]")

        where:
        initialEntitiesCount | eventsCount
        100_000              | 10_000
    }
}
