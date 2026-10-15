package com.epam.gymcrm.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Base class for DAO integration tests. One Spring context with H2 is shared by all tests;
 * the tables are emptied before every test.
 */
public abstract class JpaTestBase {

    private static final AnnotationConfigApplicationContext CONTEXT = createContext();

    protected TransactionTemplate tx;
    protected JdbcTemplate jdbc;

    private static AnnotationConfigApplicationContext createContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("test", Map.of("hibernate.hbm2ddl.auto", "create-drop")));
        context.register(TestDbConfig.class);
        context.refresh();
        return context;
    }

    protected static <T> T bean(Class<T> type) {
        return CONTEXT.getBean(type);
    }

    @BeforeEach
    protected void cleanDatabase() {
        tx = new TransactionTemplate(CONTEXT.getBean(PlatformTransactionManager.class));
        jdbc = new JdbcTemplate(CONTEXT.getBean(DataSource.class));
        for (String table : List.of("training", "trainee_trainer", "trainee", "trainer", "users", "training_type")) {
            jdbc.execute("delete from " + table);
        }
        jdbc.execute("insert into training_type (name) values ('YOGA'), ('FITNESS')");
    }

    protected int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }
}
