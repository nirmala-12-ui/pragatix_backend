package jjcet.PragatiX.integrations.neopat.config.database;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Dedicated Secondary DataSource configuration for Neopat SMS Database (neopa_sms).
 * Exclusively manages Neopat assessment and parent SMS entities and repositories.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "jjcet.PragatiX.integrations.neopat.repository",
        entityManagerFactoryRef = "neopatEntityManagerFactory",
        transactionManagerRef = "neopatTransactionManager"
)
public class NeopatDbConfig {

    @Bean(name = "neopatDataSourceProperties")
    @ConfigurationProperties("neopat.datasource")
    public DataSourceProperties neopatDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "neopatDataSource")
    public DataSource neopatDataSource(
            @Qualifier("neopatDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "neopatEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean neopatEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("neopatDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.show_sql", false);
        properties.put("hibernate.jdbc.time_zone", "Asia/Kolkata");
        properties.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        properties.put("hibernate.implicit_naming_strategy", "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy");

        return builder
                .dataSource(dataSource)
                .packages("jjcet.PragatiX.integrations.neopat.entity")
                .persistenceUnit("neopatPU")
                .properties(properties)
                .build();
    }

    @Bean(name = "neopatTransactionManager")
    public PlatformTransactionManager neopatTransactionManager(
            @Qualifier("neopatEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
