package com.distri.proyectoVenta.Config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
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
    @Value("${cache.ttl.productos:10}")     private long ttlProductosMin;
    @Value("${cache.ttl.categorias:60}")    private long ttlCategoriasMin;
    @Value("${cache.ttl.proveedores:30}")   private long ttlProveedoresMin;
    @Value("${cache.ttl.clientes:15}")      private long ttlClientesMin;
    @Value("${cache.ttl.ventas:15}") private long ttlVentasMin;

    private ObjectMapper buildRedisObjectMapper() {
        ObjectMapper om = new ObjectMapper();

        // Soporte de fechas java.time
        om.registerModule(new JavaTimeModule());

        // Importante para usar nombres de parámetros de constructores
        om.registerModule(new ParameterNamesModule());

        // Serializar fechas como texto y no timestamps
        om.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Tipado polimórfico para poder deserializar colecciones, PageImpl, Pageable, Sort, etc.
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
                .entryTtl(Duration.ofMinutes(5));
    }

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory cf,
                                     RedisCacheConfiguration defaultCfg) {
        Map<String, RedisCacheConfiguration> cfgs = new HashMap<>();
        cfgs.put("productos",     defaultCfg.entryTtl(Duration.ofMinutes(ttlProductosMin)));
        cfgs.put("categorias",    defaultCfg.entryTtl(Duration.ofMinutes(ttlCategoriasMin)));
        cfgs.put("proveedores",   defaultCfg.entryTtl(Duration.ofMinutes(ttlProveedoresMin)));
        cfgs.put("clientes",      defaultCfg.entryTtl(Duration.ofMinutes(ttlClientesMin)));
        cfgs.put("ventas", defaultCfg.entryTtl(Duration.ofMinutes(ttlVentasMin)));

        RedisCacheManager base = RedisCacheManager.builder(cf)
                .cacheDefaults(defaultCfg)
                .withInitialCacheConfigurations(cfgs)
                .build();

        return new LoggingCacheManager(base);
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

    /** CacheManager decorado que loguea todas las operaciones */
    public static class LoggingCacheManager implements CacheManager {
        private final CacheManager delegate;
        public LoggingCacheManager(CacheManager delegate) { this.delegate = delegate; }

        @Override public Cache getCache(String name) {
            Cache c = delegate.getCache(name);
            return (c == null) ? null : new LoggingCache(name, c);
        }
        @Override public java.util.Collection<String> getCacheNames() { return delegate.getCacheNames(); }
    }

    public static class LoggingCache implements Cache {
        private final String name;
        private final Cache delegate;
        private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger("com.distri.cache");

        LoggingCache(String name, Cache delegate) { this.name = name; this.delegate = delegate; }

        @Override public String getName() { return name; }
        @Override public Object getNativeCache() { return delegate.getNativeCache(); }

        @Override public ValueWrapper get(Object key) {
            ValueWrapper v = delegate.get(key);
            log.debug("[CACHE GET] {} key={}", name, key);
            return v;
        }

        @Override public <T> T get(Object key, Class<T> type) {
            T v = delegate.get(key, type);
            log.debug("[CACHE GET] {} key={} type={}", name, key, type.getSimpleName());
            return v;
        }

        @Override public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
            try {
                T v = delegate.get(key, valueLoader);
                log.debug("[CACHE GET/LOAD] {} key={}", name, key);
                return v;
            } catch (Exception e) { throw new RuntimeException(e); }
        }

        @Override public void put(Object key, Object value) {
            delegate.put(key, value);
            log.debug("[CACHE PUT] {} key={}", name, key);
        }

        @Override public ValueWrapper putIfAbsent(Object key, Object value) {
            ValueWrapper v = delegate.putIfAbsent(key, value);
            log.debug("[CACHE PUT_IF_ABSENT] {} key={}", name, key);
            return v;
        }

        @Override public void evict(Object key) {
            delegate.evict(key);
            log.debug("[CACHE DEL] {} key={}", name, key);
        }

        @Override public void clear() {
            delegate.clear();
            log.debug("[CACHE CLEAR] {}", name);
        }
    }
}
