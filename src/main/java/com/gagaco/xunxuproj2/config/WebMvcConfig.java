package com.gagaco.xunxuproj2.config;

import org.modelmapper.ModelMapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.thymeleaf.extras.springsecurity4.dialect.SpringSecurityDialect;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.spring4.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

/**
 * 2019-4-19 22:33:54
 * @author wangjiajia
 *
 * WEB相关的配置类
 *
 * 笔记
 *
 * WebMvcConfigurerAdapter
 *
 * ApplicationContextAware
 * 让我们可以获取到 ApplicationContext
 *
 * SpringResourceTemplateResover
 *
 * SpringTemplateEngine
 *
 * ThymeleafViewResover
 *
 * 覆盖WebMvcConfigurerAdapter的addResourceHandlers(ResourceHandlerRegistry registry)方法
 *
 *
 *
 */
@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    /**
     * 默认是true, 通过@Value注解把它设置成false
     */
    @Value("${spring.thymeleaf.cache}")
    private boolean thymeleafCacheEnable = true;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    /**
     * spring boot 的资源 模板解析器
     *
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.thymeleaf")
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setApplicationContext(this.applicationContext);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(thymeleafCacheEnable);
        return templateResolver;
    }


    /**
     * spring boot 的 模板引擎
     *
     */
    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        //设置模板引擎支持 Spring EL 表达式
        templateEngine.setEnableSpringELCompiler(true);
        //增加对spring security方言的支持
        SpringSecurityDialect springSecurityDialect = new SpringSecurityDialect();
        templateEngine.addDialect(springSecurityDialect);
        return templateEngine;
    }

    /**
     * thymeleaf 的 视图解析器
     *
     */
    @Bean
    public ThymeleafViewResolver viewResolver() {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine());
        return viewResolver;
    }

    /**
     * 配置静态资源的加载
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }

    /**
     * Bean Util
     */
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
