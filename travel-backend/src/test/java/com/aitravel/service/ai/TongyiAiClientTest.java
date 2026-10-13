package com.aitravel.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aitravel.config.AiProperties;
import com.aitravel.service.ai.impl.TongyiAiClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * {@link TongyiAiClient} 单元测试（详细设计 §2.4.3 / §2.4.8）。
 *
 * <p>Mock {@link RestClient} 的链式调用，绝不真实联网；{@link PromptBuilder}、
 * {@link AiResponseParser}、{@link AiProperties} 使用真实对象。
 */
class TongyiAiClientTest {

    private RestClient restClient;
    private RestClient.RequestBodyUriSpec postSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;
    private TongyiAiClient client;

    private static final String TWO_DAY_JSON = """
            {
              "title": "杭州两日游",
              "days": [
                {
                  "day": 1,
                  "theme": "西湖漫步",
                  "spots": [
                    {"name": "西湖", "time": "上午", "order": 1, "stayMinutes": 180, "tip": "带伞"}
                  ],
                  "lunch": "楼外楼",
                  "dinner": "知味观",
                  "tip": "注意防晒"
                },
                {
                  "day": 2,
                  "theme": "古镇探访",
                  "spots": [
                    {"name": "西溪湿地", "time": "全天", "order": 1, "stayMinutes": 240, "tip": ""}
                  ],
                  "lunch": "本地小吃",
                  "dinner": "河坊街",
                  "tip": ""
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        postSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(postSpec);
        when(postSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        // body 存在多个重载，显式用 any(Object.class) 匹配 body(Object)，避免误匹配 body(Body)。
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);

        AiProperties props = new AiProperties();
        props.setApiKey("test-key");
        props.setModel("qwen-turbo");
        client = new TongyiAiClient(restClient, props, new PromptBuilder(), new AiResponseParser());
    }

    private void stubResponse(String content) {
        when(responseSpec.body(TongyiAiClient.DashScopeResponse.class))
                .thenReturn(responseWithContent(content));
    }

    private TongyiAiClient.DashScopeResponse responseWithContent(String content) {
        TongyiAiClient.Message message = new TongyiAiClient.Message();
        message.setContent(content);
        TongyiAiClient.Choice choice = new TongyiAiClient.Choice();
        choice.setMessage(message);
        TongyiAiClient.Output output = new TongyiAiClient.Output();
        output.setChoices(List.of(choice));
        TongyiAiClient.DashScopeResponse response = new TongyiAiClient.DashScopeResponse();
        response.setOutput(output);
        return response;
    }

    private TripPlanRequest twoDayRequest() {
        return new TripPlanRequest("杭州", 2, "3000", "舒适",
                List.of("自然"), List.of());
    }

    @Test
    void generateTripPlanParsesStandardJson() {
        stubResponse(TWO_DAY_JSON);

        TripPlan plan = client.generateTripPlan(twoDayRequest());

        assertEquals("杭州两日游", plan.title());
        assertEquals(2, plan.days().size());
        assertEquals("西湖", plan.days().get(0).spots().get(0).name());
        assertEquals(180, plan.days().get(0).spots().get(0).stayMinutes());
        verify(bodySpec, times(1)).retrieve();
    }

    @Test
    void generateTripPlanParsesFencedJson() {
        stubResponse("```json\n" + TWO_DAY_JSON + "\n```");

        TripPlan plan = client.generateTripPlan(twoDayRequest());

        assertEquals(2, plan.days().size());
    }

    @Test
    void generateTripPlanParsesJsonWithSurroundingText() {
        stubResponse("这是攻略：\n" + TWO_DAY_JSON + "\n请查收。");

        TripPlan plan = client.generateTripPlan(twoDayRequest());

        assertEquals("杭州两日游", plan.title());
    }

    @Test
    void chatReturnsPlainTextUnchanged() {
        stubResponse("建议早上游览西湖，避开人流高峰。");

        String answer = client.chat(new ChatContext(null, null, List.of(), "怎么安排？"));

        assertEquals("建议早上游览西湖，避开人流高峰。", answer);
    }

    @Test
    void emptyChoicesThrowsAiServiceException() {
        TongyiAiClient.Output output = new TongyiAiClient.Output();
        output.setChoices(List.of());
        TongyiAiClient.DashScopeResponse response = new TongyiAiClient.DashScopeResponse();
        response.setOutput(output);
        when(responseSpec.body(TongyiAiClient.DashScopeResponse.class)).thenReturn(response);

        assertThrows(AiServiceException.class, () -> client.chat(
                new ChatContext(null, null, List.of(), "hi")));
    }

    @Test
    void nullOutputThrowsAiServiceException() {
        when(responseSpec.body(TongyiAiClient.DashScopeResponse.class))
                .thenReturn(new TongyiAiClient.DashScopeResponse());

        assertThrows(AiServiceException.class, () -> client.chat(
                new ChatContext(null, null, List.of(), "hi")));
    }

    @Test
    void readTimeoutThrowsAndDoesNotRetry() {
        when(bodySpec.retrieve()).thenThrow(new ResourceAccessException("Read timed out"));

        assertThrows(AiServiceException.class, () -> client.chat(
                new ChatContext(null, null, List.of(), "hi")));

        verify(restClient, times(1)).post();
        verify(bodySpec, times(1)).retrieve();
    }

    @Test
    void unauthorizedThrowsAiServiceException() {
        when(responseSpec.body(TongyiAiClient.DashScopeResponse.class))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertThrows(AiServiceException.class, () -> client.chat(
                new ChatContext(null, null, List.of(), "hi")));
        verify(restClient, times(1)).post();
    }

    @Test
    void tooManyRequestsThrowsAiServiceException() {
        when(bodySpec.retrieve())
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(AiServiceException.class, () -> client.chat(
                new ChatContext(null, null, List.of(), "hi")));
        verify(restClient, times(1)).post();
    }

    @Test
    void nonJsonContentThrowsAiServiceException() {
        stubResponse("抱歉，我暂时无法生成行程。");

        assertThrows(AiServiceException.class, () -> client.generateTripPlan(twoDayRequest()));
    }

    @Test
    void daysCountMismatchThrowsAiServiceException() {
        stubResponse(TWO_DAY_JSON);

        TripPlanRequest threeDayRequest = new TripPlanRequest("杭州", 3, "3000", "舒适",
                List.of("自然"), List.of());

        assertThrows(AiServiceException.class, () -> client.generateTripPlan(threeDayRequest));
    }
}
