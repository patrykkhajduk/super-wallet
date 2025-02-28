package io.hyde.wallet.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Document
public class Token {

    public static Token ofName(String name) {
        return Token.builder()
                .name(name)
                .build();
    }

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    @CreatedDate
    private String createdDate;

    @LastModifiedDate
    private String lastModifiedDate;
}
