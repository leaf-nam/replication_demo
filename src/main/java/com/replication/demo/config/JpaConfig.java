package com.replication.demo.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class JpaConfig {
    @Bean("commandDataSource") // 원본 DB와 연결된 DataSource
    public DataSource commandDataSource() {
        HikariDataSource dataSource = DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url("jdbc:mysql://localhost:3307/target_db")
                .username("master_user")
                .password("1234")
                .type(HikariDataSource.class)
                .build();
        dataSource.setMaximumPoolSize(2);
        return dataSource;
    }

    @Bean("queryDataSource") // Repl DB와 연결된 DataSource
    public DataSource queryDataSource() {
        HikariDataSource dataSource = DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url("jdbc:mysql://localhost:3308/target_db")
                .username("slave_user")
                .password("1234")
                .type(HikariDataSource.class)
                .build();
        dataSource.setMaximumPoolSize(5);
        return dataSource;
    }

    @Slf4j // AbstractRoutingDataSource 구현
    public static class ReplicationRoutingDataSource extends AbstractRoutingDataSource {
        @Override
        protected Object determineCurrentLookupKey() {
            boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("Use ReadOnly Datasource : {}", isReadOnly);
            return isReadOnly ? "replication" : "original";
        }
    }

    @Bean("routingDataSource") // DataSource 종류에 따른 DataSource 라우팅(변경)
    public DataSource routingDataSource(@Qualifier("commandDataSource") DataSource commandDataSource,
                                        @Qualifier("queryDataSource") DataSource queryDataSource) {
        ReplicationRoutingDataSource routingDataSource = new ReplicationRoutingDataSource();

        // DataSource 라우팅
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put("command", commandDataSource);
        dataSourceMap.put("query", queryDataSource);

        // 기본 DataSource 및 ReadOnly 여부에 따른 DataSource 설정
        routingDataSource.setDefaultTargetDataSource(commandDataSource);
        routingDataSource.setTargetDataSources(dataSourceMap);

        return routingDataSource;
    }

    @Bean("routingLazyDataSource")  // Connection 시점에 DataSource 결정하기 위한 Proxy
    public DataSource routingLazyDataSource(@Qualifier("routingDataSource") DataSource routingDataSource) {
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }

    @Bean("entityManagerFactory") // Entity 를 관리하기 위한 JPA Manager 설정
    LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("routingLazyDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();

        // DataSource 설정
        emf.setDataSource(dataSource);

        // EntityManager 가 관리할 Base Package 설정
        emf.setPackagesToScan("com.replication.demo.*");

        // Hibernate Vendor Adaptor 설정
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
        hibernateJpaVendorAdapter.setDatabasePlatform("org.hibernate.dialect.MySQLDialect");
        hibernateJpaVendorAdapter.setGenerateDdl(true);
        hibernateJpaVendorAdapter.setShowSql(true);
        emf.setJpaVendorAdapter(hibernateJpaVendorAdapter);

        // JPA 및 Hibernate 설정
        Properties properties = new Properties();
        properties.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        properties.setProperty("spring.jpa.properties.hibernate.show_sql","true");
        properties.setProperty("spring.jpa.properties.hibernate.format_sql","true");
        properties.setProperty("spring.jpa.properties.hibernate.default_batch_fetch_size", "100");
        emf.setJpaProperties(properties);

        return emf;
    }

    @Bean  // 트랜잭션 매니저 설정
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
        jpaTransactionManager.setEntityManagerFactory(entityManagerFactory);
        return jpaTransactionManager;
    }
}
