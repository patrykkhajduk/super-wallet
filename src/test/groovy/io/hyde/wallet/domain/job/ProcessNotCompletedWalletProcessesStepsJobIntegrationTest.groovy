package io.hyde.wallet.domain.job

import io.hyde.wallet.BaseIntegrationTest
import io.hyde.wallet.domain.model.Wallet
import io.hyde.wallet.domain.model.command.DepositFundsCommand
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.WalletSnapshot
import io.hyde.wallet.utils.TestUtils
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Subject

class ProcessNotCompletedWalletProcessesStepsJobIntegrationTest extends BaseIntegrationTest {

    private static final String BTC = "BTC"

    @Autowired
    @Subject
    private ProcessNotCompletedWalletProcessesStepsJob job

    def "should send remaining executed commands events"() {
        given:
        Wallet wallet = testHelper.initWallet()

        DepositFundsCommand command1 = new DepositFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), BTC, 11.11)
        DepositFundsCommand command2 = new DepositFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), BTC, 22.22)
        DepositFundsCommand command3 = new DepositFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), BTC, 33.33)

        and:
        testHelper.storeIncompleteWalletProcesses(wallet, command1, command2, command3)

        when:
        job.processNotCompletedWalletProcessesEvents()

        then:
        testHelper.verifyFundsAddedEvent(
                command1.getId(),
                BTC,
                command1.getAmount(),
                new WalletSnapshot(
                        wallet.getId(),
                        wallet.getOwnerId(),
                        [new WalletSnapshot.FundSnapshot(BTC, command1.getAmount(), BigDecimal.ZERO)]))
        testHelper.verifyFundsAddedEvent(
                command2.getId(),
                BTC,
                command2.getAmount(),
                new WalletSnapshot(
                        wallet.getId(),
                        wallet.getOwnerId(),
                        [new WalletSnapshot.FundSnapshot(BTC, command1.getAmount() + command2.getAmount(), BigDecimal.ZERO)]))
        testHelper.verifyFundsAddedEvent(
                command3.getId(),
                BTC,
                command3.getAmount(),
                new WalletSnapshot(
                        wallet.getId(),
                        wallet.getOwnerId(),
                        [new WalletSnapshot.FundSnapshot(BTC, command1.getAmount() + command2.getAmount() + command3.getAmount(), BigDecimal.ZERO)]))

        and:
        Wallet updatedWallet = testHelper.getWallet(wallet.getId())
        TestUtils.verifyFund(updatedWallet, BTC, command1.getAmount() + command2.getAmount() + command3.getAmount())

        and:
        testHelper.verifyWalletProcessIsCompleted(command1.getId(), wallet.getId())
        testHelper.verifyWalletProcessIsCompleted(command2.getId(), wallet.getId())
        testHelper.verifyWalletProcessIsCompleted(command3.getId(), wallet.getId())
    }
}
