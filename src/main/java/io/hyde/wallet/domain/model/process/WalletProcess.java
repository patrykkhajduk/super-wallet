package io.hyde.wallet.domain.model.process;


import com.google.common.base.Preconditions;
import io.hyde.wallet.domain.model.Wallet;
import io.hyde.wallet.domain.model.command.WalletCommand;
import io.hyde.wallet.domain.model.command.result.WalletCommandResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Document
@CompoundIndex(name = "wallet_command_send_id", def = "{'walletId': 1, 'commandId': 1, 'completed': 1}", unique = true)
@CompoundIndex(name = "created_date_send", def = "{'createdDate': 1, 'completed': 1}")
public class WalletProcess {

    @RequiredArgsConstructor
    public enum WalletProcessStep {
        SEND_RESPONSE,
        EXECUTE_COMMAND(SEND_RESPONSE);

        private final WalletProcessStep next;

        WalletProcessStep() {
            this.next = null;
        }

        public Optional<WalletProcessStep> next() {
            return Optional.ofNullable(next);
        }
    }

    public static WalletProcess fromWalletCommand(WalletCommand walletCommand, Wallet wallet) {
        return new WalletProcess(wallet, walletCommand);
    }

    @Id
    private String id;
    @Version
    private Integer version;
    //Storing wallet id and command id as separate fields for easier querying and indexing
    private String walletId;
    private String commandId;
    private WalletCommand walletCommand;
    @Setter
    private WalletCommandResult walletCommandResult;
    private List<WalletProcessExecution> stepsExecutionHistory;
    private boolean completed;
    @CreatedDate
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    private WalletProcess(Wallet wallet, WalletCommand walletCommand) {
        this.walletId = wallet.getId();
        this.commandId = walletCommand.getId();
        this.walletCommand = walletCommand;
        this.stepsExecutionHistory = new ArrayList<>();
    }

    public void markStepAsCompleted(WalletProcessStep step, Clock clock) {
        stepsExecutionHistory.add(new WalletProcessExecution(step, LocalDateTime.now(clock)));
        if (getNextStep().isEmpty()) {
            markAsCompleted();
        }
    }

    public Optional<WalletProcessStep> getNextStep() {
        return stepsExecutionHistory.isEmpty() ?
                Optional.of(Arrays.asList(WalletProcessStep.values()).getLast()) :
                stepsExecutionHistory.getLast().getStep().next();
    }

    public void markAsCompleted() {
        Preconditions.checkState(!completed, "Process already completed");
        Preconditions.checkState(getNextStep().isEmpty(), "Cannot complete process without executing all steps");
        this.completed = true;
    }

    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    public static class WalletProcessExecution {

        private WalletProcessStep step;
        private LocalDateTime completionDate;
    }

}
