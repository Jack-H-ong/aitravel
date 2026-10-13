package com.aitravel.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** {@link AiResponseParser} 单元测试（详细设计 §2.4.5 / §2.4.8）。 */
class AiResponseParserTest {

    private final AiResponseParser parser = new AiResponseParser();

    private static final String TWO_DAY_JSON = """
            {
              "title": "杭州两日游",
              "days": [
                {
                  "day": 1,
                  "theme": "西湖漫步",
                  "spots": [
                    {"name": "西湖", "time": "上午", "order": 1, "stayMinutes": 180, "tip": "带伞"},
                    {"name": "灵隐寺", "time": "下午", "order": 2, "stayMinutes": 120, "tip": ""}
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

    @Test
    void parsesStandardJson() {
        TripPlan plan = parser.parseTripPlan(TWO_DAY_JSON, 2);

        assertEquals("杭州两日游", plan.title());
        assertEquals(2, plan.days().size());
        TripPlanDay firstDay = plan.days().get(0);
        assertEquals(1, firstDay.day());
        assertEquals("西湖漫步", firstDay.theme());
        assertEquals(2, firstDay.spots().size());
        assertEquals("西湖", firstDay.spots().get(0).name());
        assertEquals(180, firstDay.spots().get(0).stayMinutes());
        assertEquals("楼外楼", firstDay.lunch());
        assertEquals("知味观", firstDay.dinner());
        assertEquals("注意防晒", firstDay.tip());
    }

    @Test
    void stripsMarkdownCodeFence() {
        String fenced = "```json\n" + TWO_DAY_JSON + "\n```";

        TripPlan plan = parser.parseTripPlan(fenced, 2);

        assertEquals("杭州两日游", plan.title());
    }

    @Test
    void extractsJsonFromSurroundingText() {
        String noisy = "好的，这是为您生成的攻略：\n" + TWO_DAY_JSON + "\n希望对您有帮助！";

        TripPlan plan = parser.parseTripPlan(noisy, 2);

        assertEquals(2, plan.days().size());
    }

    @Test
    void stripCodeFenceRemovesFence() {
        String fenced = "```json\n{\"a\":1}\n```";

        assertEquals("{\"a\":1}", parser.stripCodeFence(fenced));
    }

    @Test
    void stripCodeFenceLeavesPlainTextUntouched() {
        assertEquals("{\"a\":1}", parser.stripCodeFence("  {\"a\":1}  "));
    }

    @Test
    void missingStayMinutesDefaultsToSixty() {
        String json = """
                {
                  "title": "无停留时间",
                  "days": [
                    {
                      "day": 1,
                      "theme": "",
                      "spots": [
                        {"name": "某景点", "time": "上午", "order": 1, "tip": ""}
                      ],
                      "lunch": "",
                      "dinner": "",
                      "tip": ""
                    }
                  ]
                }
                """;

        TripPlan plan = parser.parseTripPlan(json, 1);

        assertEquals(60, plan.days().get(0).spots().get(0).stayMinutes());
    }

    @Test
    void missingStringFieldsDefaultToEmptyAndDayOrderFallBack() {
        String json = """
                {
                  "days": [
                    { "spots": [ { "name": "景点A" } ] }
                  ]
                }
                """;

        TripPlan plan = parser.parseTripPlan(json, 1);

        assertEquals("", plan.title());
        TripPlanDay day = plan.days().get(0);
        assertEquals(1, day.day());
        assertEquals("", day.theme());
        assertEquals(1, day.spots().get(0).order());
        assertEquals("景点A", day.spots().get(0).name());
    }

    @Test
    void emptyDaysThrows() {
        String json = "{\"title\":\"x\",\"days\":[]}";

        assertThrows(AiServiceException.class, () -> parser.parseTripPlan(json, 1));
    }

    @Test
    void daysCountMismatchThrows() {
        assertThrows(AiServiceException.class, () -> parser.parseTripPlan(TWO_DAY_JSON, 3));
    }

    @Test
    void nonJsonThrows() {
        assertThrows(AiServiceException.class,
                () -> parser.parseTripPlan("抱歉，我无法生成行程。", 1));
    }

    @Test
    void malformedJsonThrows() {
        assertThrows(AiServiceException.class,
                () -> parser.parseTripPlan("{\"title\":\"x\",\"days\":[}", 1));
    }

    @Test
    void nullContentThrows() {
        AiServiceException ex = assertThrows(AiServiceException.class,
                () -> parser.parseTripPlan(null, 1));
        assertTrue(ex.getDetail().contains("未找到 JSON"));
    }
}
