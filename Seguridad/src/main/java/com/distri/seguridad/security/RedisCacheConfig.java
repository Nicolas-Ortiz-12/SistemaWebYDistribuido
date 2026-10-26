package com.distri.seguridad.security;

import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisCacheConfig {

    // La cache de roles guarda texto plano: nada de serialización Java ni tipos polimórficos,
    // así un valor escrito en Redis por un tercero no puede instanciar clases arbitrarias.
    @Bean
    RedisCacheManagerBuilderCustomizer rolesCacheCustomizer(CacheProperties props) {
        CacheProperties.Redis redis = props.getRedis();
        RedisCacheConfiguration cfg = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .disableCachingNullValues();
        if (redis.getTimeToLive() != null) cfg = cfg.entryTtl(redis.getTimeToLive());
        if (redis.isUseKeyPrefix() && redis.getKeyPrefix() != null) cfg = cfg.prefixCacheNameWith(redis.getKeyPrefix());
        RedisCacheConfiguration rolesCfg = cfg;
        return builder -> builder.withCacheConfiguration(RolesCache.CACHE, rolesCfg);
    }
}
