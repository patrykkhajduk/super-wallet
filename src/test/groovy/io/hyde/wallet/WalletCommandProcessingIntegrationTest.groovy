package io.hyde.wallet

import io.hyde.wallet.domain.model.Wallet
import io.hyde.wallet.domain.model.command.BlockFundsCommand
import io.hyde.wallet.domain.model.command.DepositFundsCommand
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.BlockFundsCommandEvent
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.DepositFundsCommandEvent
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.ReleaseFundsCommandEvent
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.WithdrawFundsCommandEvent
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.WalletSnapshot
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.WalletSnapshot.FundSnapshot
import io.hyde.wallet.utils.TestUtils
import spock.util.concurrent.PollingConditions

class WalletCommandProcessingIntegrationTest extends BaseIntegrationTest {

    private static final String BTC = "BTC"
    private static final String ETH = "ETH"

    def setup() {
        testHelper.initToken(BTC)
        testHelper.initToken(ETH)
    }

    def "should deposit funds"() {
        given:
        BigDecimal amountBtc = 11.11
        BigDecimal amountEth = 22.22

        and:
        Wallet wallet = testHelper.initWallet()

        and:
        DepositFundsCommandEvent btcEvent = new DepositFundsCommandEvent(
                UUID.randomUUID().toString(), wallet.getId(), BTC, amountBtc)
        DepositFundsCommandEvent ethEvent = new DepositFundsCommandEvent(
                UUID.randomUUID().toString(), wallet.getId(), ETH, amountEth)

        when:
        testHelper.sendWalletCommandEvents(btcEvent, ethEvent)

        then:
        testHelper.verifyFundsAddedEvent(
                btcEvent.id(),
                BTC,
                amountBtc,
                new WalletSnapshot(
                        wallet.getId(),
                        wallet.getOwnerId(),
                        [new FundSnapshot(BTC, amountBtc, BigDecimal.ZERO)]))
        testHelper.verifyFundsAddedEvent(
                ethEvent.id(),
                ETH,
                amountEth,
                new WalletSnapshot(
                        wallet.getId(),
                        wallet.getOwnerId(),
                        [new FundSnapshot(BTC, amountBtc, BigDecimal.ZERO), new FundSnapshot(ETH, amountEth, BigDecimal.ZERO)]))

        and:
        Wallet updatedWallet = testHelper.getWallet(wallet.getId())
        updatedWallet.getLastExecutedCommandId().get() == ethEvent.id()
        updatedWallet.funds.size() == 2
        TestUtils.verifyFund(updatedWallet, BTC, amountBtc)
        TestUtils.verifyFund(updatedWallet, ETH, amountEth)

        and:
        testHelper.verifyWalletProcessIsCompleted(btcEvent.id(), wallet.getId())
        testHelper.verifyWalletProcessIsCompleted(ethEvent.id(), wallet.getId())
    }

    def "should block funds"() {
        given:
        BigDecimal initialAmount = 11.11
        BigDecimal blockedAmount = 1.10

        and:
        Wallet wallet = testHelper.initWallet()
        testHelper.executeCommands(
                wallet,
                new DepositFundsCommand(UUID.randomUUID().toString(), wallet.getId(), BTC, initialAmount))

        and:
        BlockFundsCommandEvent event = new BlockFundsCommandEvent(
                UUID.randomUUID().toString(), wallet.getId(), BTC, blockedAmount)

        when:
        testHelper.sendWalletCommandEvents(event)

        then:
        String lockId = testHelper.verifyFundsBlockedEvent(
                event.id(),
                BTC,
                blockedAmount,
                new WalletSnapshot(
                        wallet.getId(),
                        wallet.getOwnerId(),
                        [new FundSnapshot(BTC, initialAmount - blockedAmount, blockedAmount)]))

        and:
        Wallet updatedWallet = testHelper.getWallet(wallet.getId())
        updatedWallet.getLastExecutedCommandId().get() == event.id()
        updatedWallet.funds.size() == 1
        TestUtils.verifyFund(updatedWallet, BTC, initialAmount - blockedAmount, [(lockId): blockedAmount])

        and:
        testHelper.verifyWalletProcessIsCompleted(event.id(), wallet.getId())
    }

    def "should release blocked funds"() {
        given:
        BigDecimal initialAmount = 11.11
        BigDecimal blockedAmount = 1.10

        and:
        Wallet wallet = testHelper.initWallet()
        wallet = testHelper.executeCommands(
                wallet,
                new DepositFundsCommand(UUID.randomUUID().toString(), wallet.getId(), BTC, initialAmount),
                new BlockFundsCommand(UUID.randomUUID().toString(), wallet.getId(), BTC, blockedAmount))
        String lockId = getFundLock(wallet, BTC)

        and:
        ReleaseFundsCommandEvent event = new ReleaseFundsCommandEvent(
                UUID.randomUUID().toString(), wallet.getId(), lockId)

        when:
        testHelper.sendWalletCommandEvents(event)

        then:
        testHelper.verifyFundsReleasedEvent(
                event.id(),
                lockId,
                BTC,
                blockedAmount,
                new WalletSnapshot(
                        wallet.getId(),
                        wallet.getOwnerId(),
                        [new FundSnapshot(BTC, initialAmount, BigDecimal.ZERO)]))

        and:
        Wallet updatedWallet = testHelper.getWallet(wallet.getId())
        updatedWallet.getLastExecutedCommandId().get() == event.id()
        updatedWallet.funds.size() == 1
        TestUtils.verifyFund(updatedWallet, BTC, initialAmount)

        and:
        testHelper.verifyWalletProcessIsCompleted(event.id(), wallet.getId())
    }

    def "should withdraw blocked funds"() {
        given:
        BigDecimal initialAmount = 11.11
        BigDecimal blockedAmount = 1.10

        and:
        Wallet wallet = testHelper.initWallet()
        wallet = testHelper.executeCommands(
                wallet,
                new DepositFundsCommand(UUID.randomUUID().toString(), wallet.getId(), BTC, initialAmount),
                new BlockFundsCommand(UUID.randomUUID().toString(), wallet.getId(), BTC, blockedAmount))
        String lockId = getFundLock(wallet, BTC)

        and:
        WithdrawFundsCommandEvent event = new WithdrawFundsCommandEvent(
                UUID.randomUUID().toString(), wallet.getId(), lockId)

        when:
        testHelper.sendWalletCommandEvents(event)

        then:
        testHelper.verifyFundsWithdrawnEvent(
                event.id(),
                lockId,
                BTC,
                blockedAmount,
                new WalletSnapshot(
                        wallet.getId(),
                        wallet.getOwnerId(),
                        [new FundSnapshot(BTC, initialAmount - blockedAmount, BigDecimal.ZERO)]))

        and:
        Wallet updatedWallet = testHelper.getWallet(wallet.getId())
        updatedWallet.getLastExecutedCommandId().get() == event.id()
        updatedWallet.funds.size() == 1
        TestUtils.verifyFund(updatedWallet, BTC, initialAmount - blockedAmount)

        and:
        testHelper.verifyWalletProcessIsCompleted(event.id(), wallet.getId())
    }

    def "should not process command event when other not send event already exists for wallet"() {
        given:
        BigDecimal initialAmount = 11.11

        and:
        Wallet wallet = testHelper.initWallet()
        DepositFundsCommand notSendCommand =
                new DepositFundsCommand(UUID.randomUUID().toString(), wallet.getId(), BTC, initialAmount)
        wallet = testHelper.executeCommands(wallet, notSendCommand)
        testHelper.storeIncompleteWalletProcess(wallet, notSendCommand)

        and:
        DepositFundsCommandEvent event = new DepositFundsCommandEvent(
                UUID.randomUUID().toString(), wallet.getId(), BTC, 22.22)

        when:
        testHelper.sendWalletCommandEvents(event)

        then:
        new PollingConditions(timeout: 5).eventually {
            testHelper.verifyWalletProcessIsNotCompleted(event.id(), wallet.getId())
        }

        and:
        Wallet updatedWallet = testHelper.getWallet(wallet.getId())
        updatedWallet.funds.size() == 1
        TestUtils.verifyFund(updatedWallet, BTC, initialAmount)

        and:
        testHelper.getWalletProcessCount() == 2
        testHelper.verifyWalletProcessIsNotCompleted(notSendCommand.getId(), wallet.getId())
        testHelper.verifyWalletProcessIsNotCompleted(event.id(), wallet.getId())
    }

    def "should not execute already executed and stored command"() {
        given:
        BigDecimal initialAmount = 11.11

        and:
        Wallet wallet = testHelper.initWallet()
        DepositFundsCommand executedDepositFundsCommand = new DepositFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), BTC, initialAmount)
        wallet = testHelper.executeCommands(wallet, executedDepositFundsCommand)
        testHelper.storeCompletedWalletProcess(executedDepositFundsCommand, wallet)

        and:
        DepositFundsCommandEvent event = new DepositFundsCommandEvent(
                executedDepositFundsCommand.getId(), wallet.getId(), BTC, 1.11)

        when:
        testHelper.sendWalletCommandEvents(event)

        then:
        testHelper.verifyNoEventSent()

        and:
        Wallet updatedWallet = testHelper.getWallet(wallet.getId())
        updatedWallet.getLastExecutedCommandId().get() == executedDepositFundsCommand.getId()
        updatedWallet.funds.size() == 1
        TestUtils.verifyFund(updatedWallet, BTC, initialAmount)

        and:
        testHelper.getWalletProcessCount() == 1
    }

    def "should send event to dlt when is invalid"() {
        given:
        Wallet wallet = testHelper.initWallet()

        and:
        DepositFundsCommandEvent event = new DepositFundsCommandEvent(
                UUID.randomUUID().toString(), wallet.getId(), BTC, -1.11)

        when:
        testHelper.sendWalletCommandEvents(event)

        then:
        testHelper.getNextDltWalletCommandEvent() == event.id()
    }

    def "should send error event when wallet not found"() {
        given:
        DepositFundsCommandEvent event = new DepositFundsCommandEvent(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), BTC, 1.11)

        when:
        testHelper.sendWalletCommandEvents(event)

        then:
        testHelper.verifyErrorEvent(
                event.id(),
                event.walletId(),
                "Wallet not found: " + event.walletId())
    }

    def "should send error event when token not found"() {
        given:
        Wallet wallet = testHelper.initWallet()

        and:
        DepositFundsCommandEvent event = new DepositFundsCommandEvent(
                UUID.randomUUID().toString(), wallet.getId(), "UNKNOWN", 1.11)

        when:
        testHelper.sendWalletCommandEvents(event)

        then:
        testHelper.verifyErrorEvent(
                event.id(),
                event.walletId(),
                "Token not found: " + event.token())
    }

    def "should send error event when command cannot be executed"() {
        given:
        Wallet wallet = testHelper.initWallet()

        and:
        BlockFundsCommandEvent event = new BlockFundsCommandEvent(
                UUID.randomUUID().toString(), wallet.getId(), BTC, 1.11)

        when:
        testHelper.sendWalletCommandEvents(event)

        then:
        testHelper.verifyErrorEvent(
                event.id(),
                event.walletId(),
                "No $BTC funds to block")
    }

    private static String getFundLock(Wallet wallet, String token) {
        return wallet.funds[token].getBlocked().keySet().first()
    }
}
