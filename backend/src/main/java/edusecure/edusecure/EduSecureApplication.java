package edusecure.edusecure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {
        MongoAutoConfiguration.class,
        DataMongoAutoConfiguration.class,
        DataMongoRepositoriesAutoConfiguration.class
})
@ConfigurationPropertiesScan
@EnableJpaRepositories(
        basePackages = "edusecure.edusecure.repository",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "edusecure\\.edusecure\\.repository\\.spacechat\\..*"
        )
)
public class EduSecureApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduSecureApplication.class, args);
    }

}
