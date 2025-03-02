package io.hyde.wallet.domain.model;

import io.hyde.wallet.domain.exception.WalletCommandExecutionException;
import io.hyde.wallet.domain.model.command.BlockFundsCommand;
import io.hyde.wallet.domain.model.command.DepositFundsCommand;
import io.hyde.wallet.domain.model.command.ReleaseFundsCommand;
import io.hyde.wallet.domain.model.command.WalletCommand;
import io.hyde.wallet.domain.model.command.WithdrawFundsCommand;
import io.hyde.wallet.domain.model.command.result.BlockFundsCommandResult;
import io.hyde.wallet.domain.model.command.result.DepositFundsCommandResult;
import io.hyde.wallet.domain.model.command.result.ReleaseFundsCommandResult;
import io.hyde.wallet.domain.model.command.result.WalletCommandResult;
import io.hyde.wallet.domain.model.command.result.WithdrawFundsCommandResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
@Document
@TypeAlias("wallet")
public final class Wallet {

    public static Wallet forOwner(String ownerId) {
        return new Wallet(ownerId);
    }

    @Id
    private String id;
    @Version
    private Integer version;
    private String ownerId;
    //Storing last command result for sending wallet event in case of failure
    private WalletCommandResult lastExecutedCommandResult;
    private Map<String, Fund> funds;
    @CreatedDate
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    private Wallet(String ownerId) {
        this.ownerId = ownerId;
        this.funds = new HashMap<>();
    }

    public WalletCommandResult execute(WalletCommand command, Clock clock) {
        return getLastExecutedCommandId()
                .filter(command.getId()::equals)
                .map(commandId -> lastExecutedCommandResult)
                .orElseGet(() -> executeNewCommand(command, clock));
    }

    private WalletCommandResult executeNewCommand(WalletCommand command, Clock clock) {
        log.info("Executing {} {} on wallet {}", command.getClass().getSimpleName(), command.getId(), id);
        lastExecutedCommandResult = switch (command) {
            case DepositFundsCommand depositFundsCommand -> {
                depositFunds(depositFundsCommand.getToken(), depositFundsCommand.getAmount());
                yield DepositFundsCommandResult.from(
                        depositFundsCommand, LocalDateTime.now(clock));
            }
            case BlockFundsCommand blockFundsCommand -> {
                String lockId = blockFunds(blockFundsCommand.getToken(), blockFundsCommand.getAmount());
                yield BlockFundsCommandResult.from(
                        blockFundsCommand, lockId, LocalDateTime.now(clock));
            }
            case ReleaseFundsCommand releaseFundsCommand -> {
                Pair<String, BigDecimal> result = releaseFunds(releaseFundsCommand.getLockId());
                yield ReleaseFundsCommandResult.from(
                        releaseFundsCommand, result.getLeft(), result.getRight(), LocalDateTime.now(clock));
            }
            case WithdrawFundsCommand withdrawFundsCommand -> {
                Pair<String, BigDecimal> result = withdrawFunds(withdrawFundsCommand.getLockId());
                yield WithdrawFundsCommandResult.from(
                        withdrawFundsCommand, result.getLeft(), result.getRight(), LocalDateTime.now(clock));
            }
        };
        return lastExecutedCommandResult;
    }

    public Optional<WalletCommandResult> getLastExecutedCommandResult() {
        return Optional.ofNullable(lastExecutedCommandResult);
    }

    public Optional<String> getLastExecutedCommandId() {
        return getLastExecutedCommandResult().map(WalletCommandResult::getId);
    }

    private void depositFunds(String token, BigDecimal amount) {
        log.info("Depositing {} {} to wallet {}", amount, token, id);

        Fund fund = funds.getOrDefault(token, Fund.initial());
        fund.deposit(amount);
        funds.put(token, fund);

        log.info("Deposited {} {} to wallet {}", amount, token, id);
    }

    private String blockFunds(String token, BigDecimal amount) {
        log.info("Blocking {} {} in wallet {}", amount, token, id);

        Fund fund = funds.get(token);
        if (Objects.isNull(fund)) {
            throw new WalletCommandExecutionException("No %s funds to block".formatted(token));
        }
        String lockId = fund.block(amount);
        funds.put(token, fund);

        log.info("Blocked {} {} under lock {} in wallet {}", amount, token, lockId, id);
        return lockId;
    }

    private Pair<String, BigDecimal> releaseFunds(String lockId) {
        log.info("Releasing funds under lock {} in wallet {}", lockId, id);

        Map.Entry<String, Fund> entry = getRequiredByLockId(lockId);
        BigDecimal releasedAmount = entry.getValue().release(lockId);
        funds.put(entry.getKey(), entry.getValue());

        log.info("Released {} {} from wallet {}", releasedAmount, entry.getKey(), id);
        return Pair.of(entry.getKey(), releasedAmount);
    }

    private Pair<String, BigDecimal> withdrawFunds(String lockId) {
        log.info("Withdrawing funds under lock {} in wallet {}", lockId, id);

        Map.Entry<String, Fund> entry = getRequiredByLockId(lockId);
        BigDecimal withdrawnAmount = entry.getValue().withdraw(lockId);
        funds.put(entry.getKey(), entry.getValue());

        log.info("Withdrawn {} {} from wallet {}", withdrawnAmount, entry.getKey(), id);
        return Pair.of(entry.getKey(), withdrawnAmount);
    }

    private Map.Entry<String, Fund> getRequiredByLockId(String lockId) {
        return funds.entrySet()
                .stream()
                .filter(entry -> entry.getValue().hasLock(lockId))
                .findFirst()
                .orElseThrow(() -> new WalletCommandExecutionException("No funds found under lock " + lockId));
    }

    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Builder
    @Getter
    @TypeAlias("fund")
    public static final class Fund {

        private static final int BLOCK_LIMIT = 50;

        private static Fund initial() {
            return Fund.builder().build();
        }

        @Builder.Default
        private BigDecimal available = BigDecimal.ZERO;
        @Builder.Default
        private Map<String, BigDecimal> blocked = new HashMap<>();

        private boolean hasLock(String lockId) {
            return blocked.containsKey(lockId);
        }

        private void deposit(BigDecimal amount) {
            available = available.add(amount);
        }

        private String block(BigDecimal amount) {
            if (available.compareTo(amount) < 0) {
                throw new WalletCommandExecutionException("Not enough funds to block");
            } else if (blocked.size() >= BLOCK_LIMIT) {
                throw new WalletCommandExecutionException("Block limit exceeded");
            }
            String lockId = UUID.randomUUID().toString();
            blocked.put(lockId, amount);
            available = available.subtract(amount);
            return lockId;
        }

        private BigDecimal release(String lockId) {
            BigDecimal releasedAmount = blocked.remove(lockId);
            if (Objects.isNull(releasedAmount)) {
                throw new WalletCommandExecutionException("No funds to release under lock: " + lockId);
            }
            available = available.add(releasedAmount);
            return releasedAmount;
        }

        private BigDecimal withdraw(String lockId) {
            BigDecimal withdrawnAmount = blocked.remove(lockId);
            if (Objects.isNull(withdrawnAmount)) {
                throw new WalletCommandExecutionException("No funds to withdraw under lock: " + lockId);
            }
            return withdrawnAmount;
        }

        public BigDecimal getTotalBlocked() {
            return blocked.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
