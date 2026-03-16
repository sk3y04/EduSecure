package edusecure.edusecure.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
@ConditionalOnClass(SpringLiquibase.class)
public class LiquibaseConfig {

    @Bean(name = "liquibase")
    @ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
        return liquibase;
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

