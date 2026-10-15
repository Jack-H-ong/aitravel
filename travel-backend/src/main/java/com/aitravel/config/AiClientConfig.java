package com.aitravel.config;

import com.aitravel.service.ai.AiGateway;
import com.aitravel.service.ai.AiResponseParser;
import com.aitravel.service.ai.PromptBuilder;
import com.aitravel.service.ai.impl.TongyiAiClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * AI 网关装配（详细设计 §2.4.7）。
 *
 * <p>默认（或 {@code ai.api.provider=tongyi}）注册通义实现；新增 Provider 只需新增
 * {@code @ConditionalOnProperty} Bean，调用方不改。
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiClientConfig {

    private static final Logger log = LoggerFactory.getLogger(AiClientConfig.class);

    /** 连接超时（毫秒）。 */
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;

    /**
     * 注册通义千问 {@link AiGateway} 实现。
     *
     * @param props AI 配置
     * @param promptBuilder Prompt 构建器
     * @param responseParser 响应解析器
     * @return 通义网关
     */
    @Bean
    @ConditionalOnProperty(name = "ai.api.provider", havingValue = "tongyi", matchIfMissing = true)
    public AiGateway tongyiAiGateway(AiProperties props, PromptBuilder promptBuilder,
                                     AiResponseParser responseParser) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            log.warn("AI API key is blank — 请设置环境变量 QWEN_API_KEY（或 API_KEY）后重启，"
                    + "否则 AI 功能将返回 401 失败。");
        } else {
            log.info("AI API key loaded (provider={}, model={})", props.getProvider(),
                    props.getModel());
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MILLIS));
        requestFactory.setReadTimeout(Duration.ofMillis(props.getTimeout()));

        RestClient restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
        return new TongyiAiClient(restClient, props, promptBuilder, responseParser);
    }
}
