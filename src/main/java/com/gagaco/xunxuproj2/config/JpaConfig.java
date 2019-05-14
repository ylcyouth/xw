package com.gagaco.xunxuproj2.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * @time 2019-4-18 00:55:00
 * @time
 * @author wangjiajia
 * JPA的配置类
 *
 *
 *
 *
 *
 * 笔记
 *
 * @Configuration  表示这个JpaConfig类是一个配置类
 * 是配置类之后能怎样？
 *
 * @EnableJpaRepositories  表示。。。。
 * basePackages属性  指定要扫描的包
 *
 * @EnableTranctionManagement 表示打开事务管理
 * 打开事务管理时候能怎样？
 *
 * @Bean 表示。。。
 * 配置类的方法贴了@Bean之后能怎样？
 *
 * @ConfigutationProperties 表示。。。
 * prefix属性  。。。。
 * 配置类的方法贴了@ConfigurationProperties之后能怎样？
 *
 * DataSource
 *
 * DataSourceBuilder
 *
 * LocalContainerEntityManagerFactoryBean
 *
 * HibernateJpaVendorAdapter
 *
 * PlatformTransactionManager
 *
 * EntityMangerFactory
 *
 * JpaTransactionManager
 *
 *
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.gagaco.xunxuproj2.repository")
@EnableTransactionManagement
public class JpaConfig {


    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        return dataSourceBuilder.build();
    }

    /**
     * 视频中这个方法名为 entityManagerFactoryBean的时候启动报错了，瓦力改成 entityManagerFactory 之后启动就变好了这个为啥呢？
     * 它的名字和下面那个 transactionManager方法的 EntityManagerFactory 类型的形参的名字有关系吗？
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setGenerateDdl(false);

        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setDataSource(dataSource());
        entityManagerFactory.setJpaVendorAdapter(jpaVendorAdapter);
        entityManagerFactory.setPackagesToScan("com.gagaco.xunxuproj2.entity");
        return entityManagerFactory;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }
}
