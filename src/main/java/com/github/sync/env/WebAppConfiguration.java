package com.github.sync.env;

import com.github.sync.model.Subscription;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 应用配置类
 *
 * @author echils
 */
@Slf4j
@Configuration
public class WebAppConfiguration implements WebMvcConfigurer {


    @Bean
    public RestTemplate restTemplate(){
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(10000);
        requestFactory.setConnectTimeout(10000);
        return new RestTemplate(requestFactory);
    }


    @Bean
    @Primary
    public RedisTemplate<String, Subscription> redisTemplate(RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Subscription> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        Jackson2JsonRedisSerializer<Subscription> jsonRedisSerializer
                = new Jackson2JsonRedisSerializer<>(Subscription.class);
        jsonRedisSerializer.setObjectMapper(defaultMapper());
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }


    /**
     * 消息转换配置
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        if (converters == null) {
            converters = new ArrayList<>();
        }
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                wrapMappingJackson2HttpMessageConverter((MappingJackson2HttpMessageConverter) converter);
            }
        }
    }


    /**
     * 注册格式化工具
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToDateConverter());
    }


    /**
     * Json HttpMessageConverter
     */
    private void wrapMappingJackson2HttpMessageConverter(MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
        jackson2HttpMessageConverter.setSupportedMediaTypes(
                Collections.singletonList(MediaType.APPLICATION_JSON));
        jackson2HttpMessageConverter.setObjectMapper(defaultMapper());
    }


    /**
     * 时间转换器
     */
    @SuppressWarnings("all")
    public static class StringToDateConverter implements Converter<String, Date> {

        private static final String DAY_SEPARATOR = "-";
        private static final String DAY_PATTERN = "yyyy-MM-dd";
        private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

        @Override
        public Date convert(String source) {
            if (StringUtils.isNotBlank(source)) {
                source = source.trim();
                try {
                    if (source.contains(DAY_SEPARATOR)) {
                        SimpleDateFormat formatter = source.contains(":") ?
                                new SimpleDateFormat(DEFAULT_PATTERN) : new SimpleDateFormat(DAY_PATTERN);
                        return formatter.parse(source);
                    }
                    log.error("Date pattern is not support:{}", source);
                } catch (Exception e) {
                    log.error("String.format(\"parser %s to Date failed\", source)", e);
                    throw new RuntimeException(String.format("parser %s to Date failed", source));
                }
            }
            return null;
        }
    }

    private ObjectMapper defaultMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.enable(JsonParser.Feature.ALLOW_MISSING_VALUES);
        mapper.enable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        return mapper;
    }

}
