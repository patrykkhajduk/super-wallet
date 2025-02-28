package io.hyde.wallet.domain.job

import io.hyde.wallet.BaseIntegrationTest
import io.hyde.wallet.domain.model.Wallet
import io.hyde.wallet.domain.model.command.DepositFundsCommand
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.WalletSnapshot
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Subject

class SendExecutedCommandsEventsJobIntegrationTest extends BaseIntegrationTest {

    private static final String BTC = "BTC"

    @Autowired
    @Subject
    private SendExecutedCommandsEventsJob job

    def "should send remaining executed commands events"() {
        given:
        Wallet wallet = testHelper.initWallet()

        DepositFundsCommand command1 = new DepositFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), BTC, 11.11)
        DepositFundsCommand command2 = new DepositFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), BTC, 22.22)
        DepositFundsCommand command3 = new DepositFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), BTC, 33.33)

        wallet = testHelper.executeAndStoreSendCommands(wallet, command1)
        wallet = testHelper.executeAndStoreNotSendCommands(wallet, command2, command3)

        when:
        job.sendExecutedCommandsEvents()

        then:
        testHelper.verifyFundsAddedEvent(
                command2.id(),
                BTC,
                command2.amount(),
                new WalletSnapshot(
                        wallet.getId(),
                        wallet.getOwnerId(),
                        [new WalletSnapshot.FundSnapshot(BTC, command1.amount() + command2.amount(), BigDecimal.ZERO)]))
        testHelper.verifyFundsAddedEvent(
                command3.id(),
                BTC,
                command3.amount(),
                new WalletSnapshot(
                        wallet.getId(),
                        wallet.getOwnerId(),
                        [new WalletSnapshot.FundSnapshot(BTC, command1.amount() + command2.amount() + command3.amount(), BigDecimal.ZERO)]))

        and:
        testHelper.verifyExecutedCommandIsMarkedAsSend(command2.id(), wallet.getId())
        testHelper.verifyExecutedCommandIsMarkedAsSend(command3.id(), wallet.getId())
    }
}
