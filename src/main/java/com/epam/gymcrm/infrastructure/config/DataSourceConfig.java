package com.epam.gymcrm.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Connection to the real database (PostgreSQL) through a Hikari connection pool.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(@Value("${db.url}") String url,
                                 @Value("${db.user}") String user,
                                 @Value("${db.password}") String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }
}
