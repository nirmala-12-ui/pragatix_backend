package jjcet.PragatiX.infrastructure.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Primary DataSource configuration for existing PragatiX Database (spdms_lab).
 * Manages all existing core and modular repositories and entities.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "jjcet.PragatiX.repository",
                "jjcet.PragatiX.modules"
        },
        entityManagerFactoryRef = "pragatixEntityManagerFactory",
        transactionManagerRef = "pragatixTransactionManager"
)
public class PragatiXDbConfig {

    @Primary
    @Bean(name = "pragatixDataSourceProperties")
    @ConfigurationProperties("pragatix.datasource")
    public DataSourceProperties pragatixDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "pragatixDataSource")
    public DataSource pragatixDataSource(
            @Qualifier("pragatixDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean(name = "pragatixEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean pragatixEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("pragatixDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.show_sql", false);
        properties.put("hibernate.jdbc.time_zone", "Asia/Kolkata");
        properties.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        properties.put("hibernate.implicit_naming_strategy", "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy");

        return builder
                .dataSource(dataSource)
                .packages("jjcet.PragatiX.entity", "jjcet.PragatiX.modules")
                .persistenceUnit("pragatixPU")
                .properties(properties)
                .build();
    }

    @Primary
    @Bean(name = "pragatixTransactionManager")
    public PlatformTransactionManager pragatixTransactionManager(
            @Qualifier("pragatixEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
