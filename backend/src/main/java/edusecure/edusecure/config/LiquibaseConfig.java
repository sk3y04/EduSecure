package edusecure.edusecure.config;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
@ConditionalOnClass(SpringLiquibase.class)
public class LiquibaseConfig {

    private static final String CURRENT_CHANGESET_AUTHOR = "edusecure";
    private static final String PROJECT_CHANGELOG_PATH_PREFIX = "db/changelog/changes/%";
    private static final String DATABASE_CHANGELOG_TABLE = "databasechangelog";

    @Bean(name = "liquibase")
    @ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase() {
            @Override
            public void afterPropertiesSet() throws LiquibaseException {
                normalizeLegacyChangeSetAuthors(dataSource);
                super.afterPropertiesSet();
            }
        };
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
        return liquibase;
    }

    private static void normalizeLegacyChangeSetAuthors(DataSource dataSource) throws LiquibaseException {
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection)) {
                return;
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE databasechangelog SET author = ? WHERE filename LIKE ? AND author <> ?"
            )) {
                statement.setString(1, CURRENT_CHANGESET_AUTHOR);
                statement.setString(2, PROJECT_CHANGELOG_PATH_PREFIX);
                statement.setString(3, CURRENT_CHANGESET_AUTHOR);
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new LiquibaseException("Failed to normalize legacy Liquibase changelog author metadata", exception);
        }
    }

    private static boolean tableExists(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        return hasTable(metadata, connection.getCatalog(), DATABASE_CHANGELOG_TABLE)
                || hasTable(metadata, connection.getCatalog(), DATABASE_CHANGELOG_TABLE.toUpperCase());
    }

    private static boolean hasTable(DatabaseMetaData metadata, String catalog, String tableName) throws SQLException {
        try (ResultSet tables = metadata.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    @Bean
    static BeanFactoryPostProcessor entityManagerFactoryDependsOnLiquibasePostProcessor() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory") || !beanFactory.containsBeanDefinition("liquibase")) {
                return;
            }

            BeanDefinition entityManagerFactory = beanFactory.getBeanDefinition("entityManagerFactory");
            String[] dependsOn = entityManagerFactory.getDependsOn();
            if (dependsOn == null || dependsOn.length == 0) {
                entityManagerFactory.setDependsOn("liquibase");
                return;
            }

            boolean alreadyDependsOnLiquibase = Arrays.stream(dependsOn).anyMatch("liquibase"::equals);
            if (!alreadyDependsOnLiquibase) {
                entityManagerFactory.setDependsOn(Stream.concat(Arrays.stream(dependsOn), Stream.of("liquibase"))
                        .toArray(String[]::new));
            }
        };
    }
}

