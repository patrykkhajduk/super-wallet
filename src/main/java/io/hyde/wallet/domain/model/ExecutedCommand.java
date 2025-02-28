package io.hyde.wallet.domain.model;

import io.hyde.wallet.domain.model.command.result.WalletCommandResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Document
@CompoundIndex(name = "wallet_command_send_id", def = "{'walletId': 1, 'commandId': 1, 'send': 1}", unique = true)
@CompoundIndex(name = "created_date_send", def = "{'createdDate': 1, 'send': 1}")
//since this collection can grow quite fast TTL index can be added to auto remove old records
public class ExecutedCommand {

    public static ExecutedCommand fromLastExecutedCommand(Wallet wallet) {
        WalletCommandResult result = wallet.getLastExecutedCommandResult()
                .orElseThrow(() -> new IllegalArgumentException("Wallet has no last executed command"));
        return new ExecutedCommand(
                null,
                null,
                result.getId(),
                wallet.getId(),
                result,
                wallet,
                false,
                null,
                null);
    }

    @Id
    private String id;
    @Version
    private Integer version;
    //Storing command id and wallet id as separate fields for easier querying and indexing
    private String commandId;
    private String walletId;
    private WalletCommandResult commandResult;
    private Wallet walletSnapshot;
    private boolean send;
    @CreatedDate
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    public void markAsSend() {
        this.send = true;
    }
}
