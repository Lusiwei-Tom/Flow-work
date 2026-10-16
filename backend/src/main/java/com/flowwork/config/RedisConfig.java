package com.flowwork.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 * 自定义 RedisTemplate 序列化策略：
 * key 使用 String 序列化（可读、便于排查），value 使用 Jackson JSON 序列化
 * 避免 JDK 默认序列化产生的乱码及反序列化兼容性问题
 */
@Configuration
public class RedisConfig {

    /**
     * 定制 RedisTemplate<String, Object>：String key + JSON value
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Object> tpl = new RedisTemplate<>();
        // 设置 Redis 连接工厂
        tpl.setConnectionFactory(cf);
        // key 与 hash key 使用 String 序列化
        StringRedisSerializer keySer = new StringRedisSerializer();
        // value 与 hash value 使用 JSON 序列化
        GenericJackson2JsonRedisSerializer valSer = new GenericJackson2JsonRedisSerializer();
        tpl.setKeySerializer(keySer);
        tpl.setHashKeySerializer(keySer);
        tpl.setValueSerializer(valSer);
        tpl.setHashValueSerializer(valSer);
        // 序列化器设置完成后初始化
        tpl.afterPropertiesSet();
        return tpl;
    }
}
