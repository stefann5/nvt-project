package nvt.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@EnableCaching
public class RedisConfig {

    private GenericJackson2JsonRedisSerializer jsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonRedisSerializer());
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = jsonRedisSerializer();
        
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Vehicle caches
        cacheConfigurations.put("vehicleBrands", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("vehicleModels", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("vehicleById", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("vehiclesPage", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("vehicleSearch", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Vehicle tracking caches - IMPORTANT FOR PERFORMANCE
        cacheConfigurations.put("vehicleStatusAll", defaultConfig.entryTtl(Duration.ofMinutes(1)));
        cacheConfigurations.put("vehicleStatusOnline", defaultConfig.entryTtl(Duration.ofMinutes(1)));
        cacheConfigurations.put("vehicleLocation", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        
        // Registration request caches
        cacheConfigurations.put("pendingRequestsCount", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("requestsPage", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("requestById", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Telemetry caches - Longer TTL for better performance
        cacheConfigurations.put("distanceStats", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("availabilityStats", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Warehouse caches - IMPORTANT FOR PERFORMANCE
        cacheConfigurations.put("warehousesPage", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("warehouseSearch", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("warehouseById", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("warehouseCount", defaultConfig.entryTtl(Duration.ofMinutes(10))); // COUNT cache - longer TTL
        cacheConfigurations.put("temperatureStats", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("warehouseAvailability", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("countries", defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // Product caches - IMPORTANT FOR PERFORMANCE
        cacheConfigurations.put("productsPage", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("productSearch", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("productById", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("productCount", defaultConfig.entryTtl(Duration.ofMinutes(10))); // COUNT cache - longer TTL
        cacheConfigurations.put("productCategories", defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // Order caches
        cacheConfigurations.put("ordersPage", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("customerOrders", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("orderById", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("orderCount", defaultConfig.entryTtl(Duration.ofMinutes(10))); // COUNT cache - longer TTL

        // Authentication caches - CRITICAL FOR PERFORMANCE (every request)
        cacheConfigurations.put("tokenValidation", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("refreshTokenValidation", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("userDetails", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
