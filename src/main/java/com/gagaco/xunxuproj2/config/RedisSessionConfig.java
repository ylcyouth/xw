package com.gagaco.xunxuproj2.config;

import org.springframework.context.annotation.Configuration;

/**
 * @time 2019-4-27 19:48:01
 * @author wangjiajia
 *
 * Session配置类
 *
 * 因为没有装Redis 所以就把主题先注释掉了，以后补上
 */

@Configuration
//@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 86400)
public class RedisSessionConfig {
    /*
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
    */
}
