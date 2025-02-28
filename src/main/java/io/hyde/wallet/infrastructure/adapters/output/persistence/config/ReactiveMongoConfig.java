package io.hyde.wallet.infrastructure.adapters.output.persistence.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Configuration
@RequiredArgsConstructor
class ReactiveMongoConfig extends AbstractReactiveMongoConfiguration {

    private final MongoProperties mongoProperties;

    @Override
    protected String getDatabaseName() {
        return mongoProperties.getDatabase();
    }

    @Override
    protected boolean autoIndexCreation() {
        return mongoProperties.isAutoIndexCreation();
    }

    @Override
    protected void configureClientSettings(MongoClientSettings.Builder builder) {
        //Read concern and write concern can be hardcoded since they shouldn't be changed across environments
        builder.applyConnectionString(new ConnectionString(mongoProperties.getUri()))
                .readConcern(ReadConcern.SNAPSHOT)
                //Major with journal should be default but values in driver are different so setting this explicitly in the code
                .writeConcern(WriteConcern.MAJORITY.withJournal(true));
    }
}
