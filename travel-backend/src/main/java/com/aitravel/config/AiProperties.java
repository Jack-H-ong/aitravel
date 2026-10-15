package com.aitravel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 服务网关配置（详细设计 §2.4.1，前缀 {@code ai.api}）。
 *
 * <p>API Key 通过环境变量 {@code QWEN_API_KEY}（兼容 {@code API_KEY}）注入，禁止硬编码。
 */
@ConfigurationProperties(prefix = "ai.api")
public class AiProperties {

    /** Provider 标识，默认 tongyi。 */
    private String provider = "tongyi";

    /** 访问密钥（来自 {@code QWEN_API_KEY}，兼容 {@code API_KEY}）。 */
    private String apiKey = "";

    /** 模型名称。 */
    private String model = "qwen-max";

    /** 服务基础地址。 */
    private String baseUrl = "https://dashscope.aliyuncs.com";

    /** 读超时（毫秒）。 */
    private int timeout = 30000;

    /** 采样温度。 */
    private double temperature = 0.7;

    /** 最大生成 token 数。 */
    private int maxTokens = 3000;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
}
