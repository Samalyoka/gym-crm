package com.epam.gymcrm.support;

import com.epam.gymcrm.infrastructure.config.PersistenceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Test context: the same Hibernate setup as in production, but on in-memory H2.
 */
@Configuration
@Import(PersistenceConfig.class)
@ComponentScan("com.epam.gymcrm.infrastructure.persistence")
public class TestDbConfig {

    @Bean
    public DataSource dataSource() {
        return new DriverManagerDataSource("jdbc:h2:mem:gymcrm_test;DB_CLOSE_DELAY=-1", "sa", "");
    }
}
