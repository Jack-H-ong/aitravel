package com.aitravel.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局序列化配置。
 *
 * <ul>
 *   <li>{@link LocalDateTime} 统一格式化为 {@code yyyy-MM-dd HH:mm:ss}</li>
 *   <li>{@code Long}/{@code long} 序列化为字符串，避免前端 JS 精度丢失</li>
 * </ul>
 *
 * <p>注意：{@code Long -> String} 仅影响序列化；反序列化保持默认（同时接受数字与字符串），
 * 以最小化对入参解析的影响。
 */
@Configuration
public class JacksonConfig {

    /** 日期时间格式。 */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    /**
     * 注册自定义序列化/反序列化器到 Spring Boot 的 {@code ObjectMapper}。
     *
     * @return Jackson 构建器定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.serializerByType(LocalDateTime.class,
                    new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
            builder.deserializerByType(LocalDateTime.class,
                    new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
