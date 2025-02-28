package io.hyde.wallet.domain.job

import io.hyde.wallet.BaseIntegrationTest
import io.hyde.wallet.domain.model.Wallet
import io.hyde.wallet.domain.model.command.DepositFundsCommand
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.WalletSnapshot
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Subject

class ProcessMissingExecutedCommandsJobTest extends BaseIntegrationTest {

    private static final String BTC = "BTC"

    @Autowired
    @Subject
    private ProcessMissingExecutedCommandsJob job

    def "should store missing executed commands"() {
        given:
        Wallet walletWithNotStoredCommands = testHelper.initWallet()
        walletWithNotStoredCommands = testHelper.executeAndStoreSendCommands(
                walletWithNotStoredCommands,
                new DepositFundsCommand(UUID.randomUUID().toString(), walletWithNotStoredCommands.getId(), BTC, 11.11))

        DepositFundsCommand notStoredCommand =
                new DepositFundsCommand(UUID.randomUUID().toString(), walletWithNotStoredCommands.getId(), BTC, 22.22)
        testHelper.executeWithoutStoringCommand(walletWithNotStoredCommands, notStoredCommand)

        Wallet walletWithStoredCommands = testHelper.initWallet()
        testHelper.executeAndStoreSendCommands(
                walletWithStoredCommands,
                new DepositFundsCommand(UUID.randomUUID().toString(), walletWithStoredCommands.getId(), BTC, 33.33))

        //Additional wallet to check if only wallets with available last executed command result are processed
        testHelper.initWallet()

        and:
        assert testHelper.getExecutedCommandsCount() == 2

        when:
        job.processMissingExecutedCommands()

        then:
        testHelper.verifyFundsAddedEvent(
                notStoredCommand.id(),
                BTC,
                notStoredCommand.amount(),
                new WalletSnapshot(
                        walletWithNotStoredCommands.getId(),
                        walletWithNotStoredCommands.getOwnerId(),
                        [new WalletSnapshot.FundSnapshot(BTC, walletWithNotStoredCommands.getFunds()[BTC].getAvailable(), BigDecimal.ZERO)]))

        and:
        testHelper.getExecutedCommandsCount() == 3
        testHelper.verifyExecutedCommandIsMarkedAsSend(notStoredCommand.id(), walletWithNotStoredCommands.getId())
    }
}
