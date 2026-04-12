package edusecure.edusecure.config.chat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@ConditionalOnProperty(prefix = "app.chat", name = "enabled", havingValue = "true")
@EnableMongoRepositories(basePackages = "edusecure.edusecure.repository.spacechat")
public class SpaceChatMongoConfiguration extends AbstractMongoClientConfiguration {

	@Value("${spring.data.mongodb.uri}")
	private String mongoUri;

	@Value("${spring.data.mongodb.database:edusecure}")
	private String databaseName;

	@Value("${spring.data.mongodb.auto-index-creation:true}")
	private boolean autoIndexCreation;

	@Override
	protected String getDatabaseName() {
		return databaseName;
	}

	@Override
	public MongoClient mongoClient() {
		return MongoClients.create(mongoUri);
	}

	@Override
	protected boolean autoIndexCreation() {
		return autoIndexCreation;
	}
}

