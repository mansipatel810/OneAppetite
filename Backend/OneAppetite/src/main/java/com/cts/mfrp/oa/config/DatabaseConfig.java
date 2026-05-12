package com.cts.mfrp.oa.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Defines two DataSources side by side:
 *
 *  • {@code dataSource} (@Primary) — the main DataSource used by JPA and every
 *    repository / service that writes to the DB. Authenticated as the full
 *    admin DB user ({@code DB_USER}).
 *
 *  • {@code readOnlyDataSource} — used only by {@link #readOnlyJdbcTemplate}.
 *    Authenticated as a separate DB user ({@code DB_READONLY_USER}) granted
 *    only SELECT on defaultdb. Wired into ChatService via @Qualifier.
 *
 * The @Primary on the main DataSource is critical: Spring Boot's
 * DataSourceAutoConfiguration is @ConditionalOnMissingBean(DataSource.class).
 * Without explicitly declaring the main DataSource here, JPA would pick the
 * readOnlyDataSource and the whole app would be locked to SELECT-only.
 */
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.readonly.username}")
    private String readOnlyUsername;

    @Value("${spring.datasource.readonly.password}")
    private String readOnlyPassword;

    /**
     * Main DataSource — full-privilege admin user. Used by JPA / Hibernate
     * for every entity write (orders, cart, register, menu CRUD, etc.).
     * DataSourceBuilder defaults to HikariCP connection pooling.
     */
    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url(dbUrl)
                .username(dbUsername)
                .password(dbPassword)
                .build();
    }

    /**
     * Read-only DataSource — chatbot user, SELECT-only at the MySQL grant
     * level. Used solely by readOnlyJdbcTemplate.
     */
    @Bean(name = "readOnlyDataSource")
    public DataSource readOnlyDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url(dbUrl)
                .username(readOnlyUsername)
                .password(readOnlyPassword)
                .build();
    }

    @Bean(name = "readOnlyJdbcTemplate")
    public JdbcTemplate readOnlyJdbcTemplate(@Qualifier("readOnlyDataSource") DataSource readOnlyDataSource) {
        return new JdbcTemplate(readOnlyDataSource);
    }
}
