package io.hyde.wallet.domain.model

import io.hyde.wallet.domain.exception.WalletCommandExecutionException
import io.hyde.wallet.domain.model.command.BlockFundsCommand
import io.hyde.wallet.domain.model.command.DepositFundsCommand
import io.hyde.wallet.domain.model.command.ReleaseFundsCommand
import io.hyde.wallet.domain.model.command.WithdrawFundsCommand
import io.hyde.wallet.domain.model.command.result.BlockFundsCommandResult
import io.hyde.wallet.domain.model.command.result.DepositFundsCommandResult
import io.hyde.wallet.domain.model.command.result.ReleaseFundsCommandResult
import io.hyde.wallet.domain.model.command.result.WithdrawFundsCommandResult
import io.hyde.wallet.utils.TestUtils
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class WalletTest extends Specification {

    private static final String BTC = "BTC"
    private static final String ETH = "ETH"
    private static final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

    def "should deposit funds to wallet"() {
        given:
        Wallet wallet = initWallet([(BTC): 11.11])

        and:
        DepositFundsCommand command = new DepositFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), BTC, 22.22)

        when:
        DepositFundsCommandResult result = wallet.execute(command, clock) as DepositFundsCommandResult

        then:
        result.id == command.id()
        result.token == command.token()
        result.amount == command.amount()
        result.timestamp == LocalDateTime.now(clock)

        and:
        TestUtils.verifyFund(wallet, BTC, 33.33)
        wallet.getLastExecutedCommandResult().get() == result
    }

    def "should block existing funds"() {
        given:
        Wallet wallet = initWallet([(BTC): 11.11])

        and:
        BlockFundsCommand command = new BlockFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), BTC, 1.11)

        when:
        BlockFundsCommandResult result = wallet.execute(command, clock) as BlockFundsCommandResult

        then:
        result.id == command.id()
        result.token == command.token()
        result.amount == command.amount()
        result.blockedFundsLockId != null
        result.timestamp == LocalDateTime.now(clock)

        and:
        TestUtils.verifyFund(wallet, BTC, 10.00, [(result.blockedFundsLockId): 1.11])
        wallet.getLastExecutedCommandResult().get() == result
    }

    def "should throw exception when blocking non existing funds"() {
        given:
        Wallet wallet = initWallet([(BTC): 11.11])

        and:
        BlockFundsCommand command = new BlockFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), ETH, 1.11)

        when:
        wallet.execute(command, clock)

        then:
        WalletCommandExecutionException exception = thrown(WalletCommandExecutionException)
        exception.getMessage() == "No $ETH funds to block"

        and:
        TestUtils.verifyFund(wallet, BTC, 11.11)
    }

    def "should throw exception when blocking more funds than available"() {
        given:
        Wallet wallet = initWallet([(BTC): 11.11])

        and:
        BlockFundsCommand command = new BlockFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), BTC, 11.12)

        when:
        wallet.execute(command, clock)

        then:
        WalletCommandExecutionException exception = thrown(WalletCommandExecutionException)
        exception.getMessage() == "Not enough funds to block"

        and:
        TestUtils.verifyFund(wallet, BTC, 11.11)
    }

    def "should release blocked funds"() {
        given:
        Wallet wallet = initWallet([(BTC): 11.11])
        String lockId1 = blockFunds(wallet, BTC, 1.11)
        String lockId2 = blockFunds(wallet, BTC, 1.00)

        and:
        ReleaseFundsCommand command = new ReleaseFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), lockId1)

        when:
        ReleaseFundsCommandResult result = wallet.execute(command, clock) as ReleaseFundsCommandResult

        then:
        result.id == command.id()
        result.lockId == lockId1
        result.releasedFundsToken == BTC
        result.releasedFundsAmount == 1.11
        result.timestamp == LocalDateTime.now(clock)

        and:
        TestUtils.verifyFund(wallet, BTC, 10.11, [(lockId2): 1.00])
        wallet.getLastExecutedCommandResult().get() == result
    }

    def "should throw exception when releasing unknown lock"() {
        given:
        Wallet wallet = initWallet([(BTC): 11.11])
        String lockId = blockFunds(wallet, BTC, 1.11)

        and:
        ReleaseFundsCommand command = new ReleaseFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), UUID.randomUUID().toString())

        when:
        wallet.execute(command, clock)

        then:
        WalletCommandExecutionException exception = thrown(WalletCommandExecutionException)
        exception.getMessage() == "No funds found under lock ${command.lockId()}"

        and:
        TestUtils.verifyFund(wallet, BTC, 10.00, [(lockId): 1.11])
    }

    def "should withdraw blocked funds"() {
        given:
        Wallet wallet = initWallet([(BTC): 11.11])
        String lockId1 = blockFunds(wallet, BTC, 1.11)
        String lockId2 = blockFunds(wallet, BTC, 1.00)

        and:
        WithdrawFundsCommand command = new WithdrawFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), lockId1)

        when:
        WithdrawFundsCommandResult result = wallet.execute(command, clock) as WithdrawFundsCommandResult

        then:
        result.id == command.id()
        result.lockId == lockId1
        result.withdrawnFundsToken == BTC
        result.withdrawnFundsAmount == 1.11
        result.timestamp == LocalDateTime.now(clock)

        and:
        TestUtils.verifyFund(wallet, BTC, 9.00, [(lockId2): 1.00])
        wallet.getLastExecutedCommandResult().get() == result
    }

    def "should throw exception when withdrawing unknown lock"() {
        given:
        Wallet wallet = initWallet([(BTC): 11.11])
        String lockId = blockFunds(wallet, BTC, 1.11)

        and:
        WithdrawFundsCommand command = new WithdrawFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), UUID.randomUUID().toString())

        when:
        wallet.execute(command, clock)

        then:
        WalletCommandExecutionException exception = thrown(WalletCommandExecutionException)
        exception.getMessage() == "No funds found under lock ${command.lockId()}"

        and:
        TestUtils.verifyFund(wallet, BTC, 10.00, [(lockId): 1.11])
    }

    def "should not execute command when last executed has same id"() {
        given:
        Wallet wallet = initWallet()

        and:
        DepositFundsCommand command = new DepositFundsCommand(
                UUID.randomUUID().toString(), wallet.getId(), BTC, 11.11)

        when:
        DepositFundsCommandResult result1 = wallet.execute(command, clock) as DepositFundsCommandResult
        DepositFundsCommandResult result2 = wallet.execute(command, clock) as DepositFundsCommandResult

        then:
        TestUtils.verifyFund(wallet, BTC, 11.11)
        result1 == result2
        wallet.getLastExecutedCommandResult().get() == result1
        wallet.getLastExecutedCommandResult().get() == result2
    }

    private static Wallet initWallet(Map<String, BigDecimal> funds = [:]) {
        Wallet wallet = Wallet.forOwner(UUID.randomUUID().toString())
        funds.each { token, amount ->
            wallet.execute(new DepositFundsCommand(UUID.randomUUID().toString(), wallet.getId(), token, amount), clock)
        }
        return wallet
    }

    private static String blockFunds(Wallet wallet, String token, BigDecimal amount) {
        BlockFundsCommand command = new BlockFundsCommand(UUID.randomUUID().toString(), wallet.getId(), token, amount)
        return (wallet.execute(command, clock) as BlockFundsCommandResult).blockedFundsLockId
    }
}
