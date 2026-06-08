package com.distri.proyectoVenta.Config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@EnableCaching
@Configuration
@RequiredArgsConstructor
public class RedisCacheConfig {
    @Value("${cache.ttl.productos:10}") private long ttlProductosMin;
    @Value("${cache.ttl.proveedores:30}") private long ttlProveedoresMin;
    @Value("${cache.ttl.clientes:15}") private long ttlClientesMin;
    @Value("${cache.ttl.ventas:15}") private long ttlVentasMin;

    private ObjectMapper buildRedisObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.registerModule(new ParameterNamesModule());
        om.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        om.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
        return om;
    }

    @Bean
    public GenericJackson2JsonRedisSerializer jacksonSerializer() {
        return new GenericJackson2JsonRedisSerializer(buildRedisObjectMapper());
    }

    @Bean
    public RedisCacheConfiguration defaultCacheConfig(GenericJackson2JsonRedisSerializer ser) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(ser))
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(10));
    }

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory cf,
                                     RedisCacheConfiguration defaultCfg) {
        Map<String, RedisCacheConfiguration> cfgs = new HashMap<>();
        cfgs.put("productos", defaultCfg.entryTtl(Duration.ofMinutes(ttlProductosMin)));
        cfgs.put("proveedores", defaultCfg.entryTtl(Duration.ofMinutes(ttlProveedoresMin)));
        cfgs.put("clientes", defaultCfg.entryTtl(Duration.ofMinutes(ttlClientesMin)));
        cfgs.put("ventas", defaultCfg.entryTtl(Duration.ofMinutes(ttlVentasMin)));

        return RedisCacheManager.builder(cf)
                .transactionAware()
                .cacheDefaults(defaultCfg)
                .withInitialCacheConfigurations(cfgs)
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf,
                                                       GenericJackson2JsonRedisSerializer ser) {
        RedisTemplate<String, Object> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(cf);
        tpl.setKeySerializer(new StringRedisSerializer());
        tpl.setHashKeySerializer(new StringRedisSerializer());
        tpl.setValueSerializer(ser);
        tpl.setHashValueSerializer(ser);
        tpl.afterPropertiesSet();
        return tpl;
    }
}
