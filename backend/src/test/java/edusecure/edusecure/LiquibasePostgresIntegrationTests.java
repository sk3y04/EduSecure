package edusecure.edusecure;

import edusecure.edusecure.entity.auth.Role;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.repository.auth.RoleRepository;
import edusecure.edusecure.repository.auth.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("postgres-liquibase")
@Testcontainers(disabledWithoutDocker = true)
class LiquibasePostgresIntegrationTests {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("edusecure")
            .withUsername("edusecure")
            .withPassword("edusecure");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.docker.compose.enabled", () -> false);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void liquibaseAppliesSchemaAndSeedsReferenceDataOnPostgres() {
        Integer changeSetCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog WHERE id = ?",
                Integer.class,
                "001-initial-schema"
        );
        assertThat(changeSetCount).isEqualTo(1);

        Integer submissionStorageChangeSetCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog WHERE id = ?",
                Integer.class,
                "002-submission-aes-storage"
        );
        assertThat(submissionStorageChangeSetCount).isEqualTo(1);

        Integer assignmentSpaceChangeSetCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog WHERE id = ?",
                Integer.class,
                "005-assignment-space-link"
        );
        assertThat(assignmentSpaceChangeSetCount).isEqualTo(1);

        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('roles', 'users', 'user_roles')",
                Integer.class
        );
        assertThat(tableCount).isEqualTo(3);

        Integer uniqueConstraintCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = 'public' AND table_name = 'users' AND constraint_name = 'uk_users_email' AND constraint_type = 'UNIQUE'",
                Integer.class
        );
        assertThat(uniqueConstraintCount).isEqualTo(1);

        Integer foreignKeyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = 'public' AND table_name = 'user_roles' AND constraint_name = 'fk_user_roles_role' AND constraint_type = 'FOREIGN KEY'",
                Integer.class
        );
        assertThat(foreignKeyCount).isEqualTo(1);

        Integer indexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'user_roles' AND indexname = 'idx_user_roles_role_id'",
                Integer.class
        );
        assertThat(indexCount).isEqualTo(1);

        Integer submissionEncryptionColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'submissions' AND column_name IN ('storage_encryption_algorithm', 'storage_encryption_nonce', 'wrapped_content_encryption_key', 'key_wrap_algorithm', 'storage_key_version', 'ciphertext_length_bytes')",
                Integer.class
        );
        assertThat(submissionEncryptionColumnCount).isEqualTo(6);

        Integer assignmentSpaceColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'assignments' AND column_name = 'space_id'",
                Integer.class
        );
        assertThat(assignmentSpaceColumnCount).isEqualTo(1);

        Integer assignmentSpaceForeignKeyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = 'public' AND table_name = 'assignments' AND constraint_name = 'fk_assignments_space' AND constraint_type = 'FOREIGN KEY'",
                Integer.class
        );
        assertThat(assignmentSpaceForeignKeyCount).isEqualTo(1);

        Integer assignmentSpaceIndexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'assignments' AND indexname = 'idx_assignments_space_id'",
                Integer.class
        );
        assertThat(assignmentSpaceIndexCount).isEqualTo(1);

        for (RoleName roleName : RoleName.values()) {
            assertThat(roleRepository.findByName(roleName)).isPresent();
        }
        assertThat(roleRepository.count()).isEqualTo(RoleName.values().length);
    }

    @Test
    void repositoriesRoundTripAgainstRealPostgres() {
        Role studentRole = roleRepository.findByName(RoleName.STUDENT).orElseThrow();

        User user = new User();
        user.setEmail("postgres-liquibase-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("bcrypt-placeholder");
        user.setFullName("Postgres Liquibase Test User");
        user.setMfaEnabled(false);
        user.setRoles(Set.of(studentRole));

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(userRepository.findByEmail(user.getEmail()))
                .isPresent()
                .get()
                .satisfies(found -> {
                    assertThat(found.getFullName()).isEqualTo("Postgres Liquibase Test User");
                    assertThat(found.getRoles())
                            .extracting(Role::getName)
                            .containsExactly(RoleName.STUDENT);
                });

        Integer joinRowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ?",
                Integer.class,
                savedUser.getId()
        );
        assertThat(joinRowCount).isEqualTo(1);
    }
}

