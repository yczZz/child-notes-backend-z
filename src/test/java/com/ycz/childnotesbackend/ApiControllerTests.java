package com.ycz.childnotesbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ycz.childnotesbackend.agent.baby.BabyAnalysisAgent;
import com.ycz.childnotesbackend.agent.baby.BabyAnalysisRequest;
import com.ycz.childnotesbackend.agent.baby.BabyAnalysisResult;
import com.ycz.childnotesbackend.mapper.ChildRecordMapper;
import com.ycz.childnotesbackend.model.entity.ChildRecord;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.datasource.type=com.alibaba.druid.pool.DruidDataSource",
        "spring.datasource.druid.url=${MYSQL_TEST_URL:jdbc:mysql://127.0.0.1:7904/child_notes_test?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true}",
        "spring.datasource.druid.username=${MYSQL_TEST_USER:${MYSQL_USER:root}}",
        "spring.datasource.druid.password=${MYSQL_TEST_PWD:${MYSQL_PWD:root}}",
        "spring.datasource.druid.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.datasource.druid.validation-query=SELECT 1",
        "rate-limit.enabled=false",
        "wechat.mini-app.app-id=",
        "wechat.mini-app.app-secret="
})
@AutoConfigureMockMvc
class ApiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChildRecordMapper childRecordMapper;

    @MockBean
    private BabyAnalysisAgent babyAnalysisAgent;

    @Test
    void authApisReturnTokenAndCurrentUser() throws Exception {
        String token = loginAndGetToken();

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("nickName", "Updated Mom");
        profile.put("avatarUrl", "https://example.com/new-avatar.png");

        mockMvc.perform(put("/api/auth/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.nickName").value("Updated Mom"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/new-avatar.png"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.nickName").value("Updated Mom"));
    }

    @Test
    void wxLoginMarksOnlyFirstLoginAsNewUser() throws Exception {
        String code = uniqueCode("new-user-flag");
        Map<String, Object> login = loginPayload(code, "Invite User");

        mockMvc.perform(post("/api/auth/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.newUser").value(true));

        mockMvc.perform(post("/api/auth/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.newUser").value(false));
    }

    @Test
    void pointsInviteRewardsNewUserOnly() throws Exception {
        String inviterToken = loginAndGetToken(uniqueCode("points-inviter"), "Points Inviter");
        String referrerId = getShareReferrerId(inviterToken);

        Map<String, Object> invitedLogin = loginPayload(uniqueCode("points-new-user"), "Points New User");
        invitedLogin.put("referrerId", referrerId);

        mockMvc.perform(post("/api/auth/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invitedLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.newUser").value(true));

        mockMvc.perform(withToken(get("/api/points/dashboard"), inviterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.points").value(100))
                .andExpect(jsonPath("$.data.inviteRecords.length()").value(1))
                .andExpect(jsonPath("$.data.inviteRecords[0].invitedNickName").value("Points New User"))
                .andExpect(jsonPath("$.data.inviteRecords[0].points").value(100));
    }

    @Test
    void pointsInviteDoesNotRewardExistingUser() throws Exception {
        String inviterToken = loginAndGetToken(uniqueCode("points-old-inviter"), "Points Old Inviter");
        String referrerId = getShareReferrerId(inviterToken);
        String existingCode = uniqueCode("points-existing-user");

        mockMvc.perform(post("/api/auth/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload(existingCode, "Points Existing User"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.newUser").value(true));

        Map<String, Object> invitedLogin = loginPayload(existingCode, "Points Existing User");
        invitedLogin.put("referrerId", referrerId);

        mockMvc.perform(post("/api/auth/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invitedLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.newUser").value(false));

        mockMvc.perform(withToken(get("/api/points/dashboard"), inviterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.points").value(0))
                .andExpect(jsonPath("$.data.inviteRecords.length()").value(0));
    }

    @Test
    void wxLoginRefreshesOldDefaultWechatProfile() throws Exception {
        String code = uniqueCode("default-profile");

        mockMvc.perform(post("/api/auth/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginPayload(code, "微信用户", "https://example.com/default-avatar.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newUser").value(true))
                .andExpect(jsonPath("$.data.userInfo.nickName", matchesPattern("^微信用户[0-9A-Za-z]{6}$")));

        mockMvc.perform(post("/api/auth/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload(code, "Fresh Mom", "https://example.com/fresh-avatar.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newUser").value(false))
                .andExpect(jsonPath("$.data.userInfo.nickName").value("Fresh Mom"))
                .andExpect(jsonPath("$.data.userInfo.avatarUrl").value("https://example.com/fresh-avatar.png"));
    }

    @Test
    void wxLoginKeepsOldCustomWechatProfile() throws Exception {
        String code = uniqueCode("custom-profile");

        mockMvc.perform(post("/api/auth/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload(code, "Custom Mom", "https://example.com/custom-avatar.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newUser").value(true))
                .andExpect(jsonPath("$.data.userInfo.nickName").value("Custom Mom"));

        mockMvc.perform(post("/api/auth/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload(code, "Fresh Mom", "https://example.com/fresh-avatar.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newUser").value(false))
                .andExpect(jsonPath("$.data.userInfo.nickName").value("Custom Mom"))
                .andExpect(jsonPath("$.data.userInfo.avatarUrl").value("https://example.com/custom-avatar.png"));
    }

    @Test
    void babyApisReturnFrontendShape() throws Exception {
        String token = loginAndGetToken();
        createBaby(token, "Initial Baby");

        mockMvc.perform(withToken(get("/api/baby/current"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.name", notNullValue()))
                .andExpect(jsonPath("$.data.birthDate", notNullValue()))
                .andExpect(jsonPath("$.data.age", notNullValue()));

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("name", "Api Test Baby");
        update.put("gender", "girl");
        update.put("birthDate", "2026-01-15");

        mockMvc.perform(withToken(put("/api/baby/update"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.name").value("Api Test Baby"))
                .andExpect(jsonPath("$.data.gender").value("girl"));

        Map<String, Object> create = new LinkedHashMap<>();
        create.put("name", "Second Baby");
        create.put("gender", "boy");
        create.put("birthDate", "2026-05-01");

        String createJson = mockMvc.perform(withToken(post("/api/baby/add"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.name").value("Second Baby"))
                .andExpect(jsonPath("$.data.gender").value("boy"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long secondBabyId = objectMapper.readTree(createJson).get("data").get("id").asLong();

        Map<String, Object> updateById = new LinkedHashMap<>();
        updateById.put("id", secondBabyId);
        updateById.put("name", "Second Baby Updated");
        updateById.put("gender", "girl");
        updateById.put("birthDate", "2026-05-01");

        mockMvc.perform(withToken(put("/api/baby/update"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateById)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.id").value(secondBabyId))
                .andExpect(jsonPath("$.data.name").value("Second Baby Updated"))
                .andExpect(jsonPath("$.data.gender").value("girl"));

        mockMvc.perform(withToken(get("/api/baby/list"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data[0].id", notNullValue()))
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(2)));
    }

    @Test
    void babyFamilyRoleIsBoundAndEditable() throws Exception {
        String token = loginAndGetToken(uniqueCode("family-role"), "Family Parent");

        Map<String, Object> create = new LinkedHashMap<>();
        create.put("name", "Family Baby");
        create.put("gender", "girl");
        create.put("birthDate", "2026-05-01");
        create.put("roleCode", "father");
        create.put("roleName", "爸爸");

        String createJson = mockMvc.perform(withToken(post("/api/baby/add"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.familyRoleCode").value("father"))
                .andExpect(jsonPath("$.data.familyRoleName").value("爸爸"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long babyId = objectMapper.readTree(createJson).get("data").get("id").asLong();

        mockMvc.perform(withToken(get("/api/baby/family/members"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data[0].babyId").value(babyId))
                .andExpect(jsonPath("$.data[0].members[0].roleCode").value("father"))
                .andExpect(jsonPath("$.data[0].members[0].roleName").value("爸爸"))
                .andExpect(jsonPath("$.data[0].members[0].owner").value(true))
                .andExpect(jsonPath("$.data[0].members[0].mine").value(true));

        Map<String, Object> updateRole = new LinkedHashMap<>();
        updateRole.put("babyId", babyId);
        updateRole.put("roleCode", "mother");
        updateRole.put("roleName", "妈妈");

        mockMvc.perform(withToken(put("/api/baby/family/my-role"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.roleCode").value("mother"))
                .andExpect(jsonPath("$.data.roleName").value("妈妈"));

        mockMvc.perform(withToken(get("/api/baby/list"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data[0].familyRoleCode").value("mother"))
                .andExpect(jsonPath("$.data[0].familyRoleName").value("妈妈"));
    }

    @Test
    void inviteJoinsAllBabiesOwnedByFamilyCreator() throws Exception {
        String ownerToken = loginAndGetToken(uniqueCode("family-owner"), "Family Owner");
        String memberToken = loginAndGetToken(uniqueCode("family-member"), "Family Member");
        Long firstBabyId = createBaby(ownerToken, "嘻嘻");
        Long secondBabyId = createBaby(ownerToken, "哈哈");

        Map<String, Object> join = new LinkedHashMap<>();
        join.put("babyId", firstBabyId);
        join.put("roleCode", "mother");
        join.put("roleName", "妈妈");

        mockMvc.perform(withToken(post("/api/baby/family/join"), memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(join)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.babyId").value(firstBabyId))
                .andExpect(jsonPath("$.data.roleCode").value("mother"));

        mockMvc.perform(withToken(get("/api/baby/list"), memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(firstBabyId))
                .andExpect(jsonPath("$.data[1].id").value(secondBabyId))
                .andExpect(jsonPath("$.data[0].familyRoleCode").value("mother"))
                .andExpect(jsonPath("$.data[1].familyRoleCode").value("mother"));
    }

    @Test
    void recordApisReturnFrontendShape() throws Exception {
        String token = loginAndGetToken();
        Long babyId = createBaby(token, "Record Baby");
        String date = LocalDate.now().toString();
        Map<String, Object> feed = new LinkedHashMap<>();
        feed.put("type", "bottle");
        feed.put("amount", 120);
        feed.put("time", date + " 08:30");

        mockMvc.perform(withBaby(withToken(post("/api/records/feed"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.babyId").value(babyId))
                .andExpect(jsonPath("$.data.type").value("bottle"))
                .andExpect(jsonPath("$.data.amount").value(120));

        Map<String, Object> sleep = new LinkedHashMap<>();
        sleep.put("startTime", date + " 09:00");

        String sleepJson = mockMvc.perform(withBaby(withToken(post("/api/records/sleep"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sleep)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long sleepId = objectMapper.readTree(sleepJson).get("data").get("id").asLong();

        mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", date), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.feeds.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.sleeps.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.diapers").isArray())
                .andExpect(jsonPath("$.data.temperatures").isArray())
                .andExpect(jsonPath("$.data.abnormals").isArray());

        mockMvc.perform(withBaby(withToken(put("/api/records/sleep/{sleepId}/wake", sleepId), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.endTime", notNullValue()))
                .andExpect(jsonPath("$.data.duration", notNullValue()));

        mockMvc.perform(withBaby(withToken(get("/api/records/today/stats"), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.timeSinceLastFeed", notNullValue()))
                .andExpect(jsonPath("$.data.todaySleepTotal", notNullValue()))
                .andExpect(jsonPath("$.data.hasFever", notNullValue()));

        mockMvc.perform(withBaby(withToken(get("/api/records/stats/date").param("date", date), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.date").value(date))
                .andExpect(jsonPath("$.data.feed.count", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.feed.totalMilk", greaterThanOrEqualTo(120)))
                .andExpect(jsonPath("$.data.sleep.count", greaterThanOrEqualTo(1)));

        mockMvc.perform(withBaby(withToken(get("/api/records/stats/range")
                        .param("startDate", date)
                        .param("endDate", date), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data[0].date").value(date))
                .andExpect(jsonPath("$.data[0].feed.totalMilk", greaterThanOrEqualTo(120)));
    }

    @Test
    void logicallyDeletedRecordsAreExcludedFromStatsAndAiSource() throws Exception {
        String token = loginAndGetToken(uniqueCode("delete-filter"), "Delete Filter Parent");
        Long babyId = createBaby(token, "Delete Filter Baby");
        String date = LocalDate.now().toString();
        String deletedMarker = "DELETED_RECORD_SHOULD_NOT_REACH_AI_" + UUID.randomUUID();

        Map<String, Object> keptFeed = new LinkedHashMap<>();
        keptFeed.put("type", "bottle");
        keptFeed.put("amount", 80);
        keptFeed.put("time", date + " 07:10");

        mockMvc.perform(withBaby(withToken(post("/api/records/feed"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(keptFeed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"));

        Map<String, Object> deletedFeed = new LinkedHashMap<>();
        deletedFeed.put("type", "bottle");
        deletedFeed.put("amount", 200);
        deletedFeed.put("note", deletedMarker);
        deletedFeed.put("time", date + " 08:20");

        String deletedFeedJson = mockMvc.perform(withBaby(withToken(post("/api/records/feed"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deletedFeed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long deletedFeedId = objectMapper.readTree(deletedFeedJson).get("data").get("id").asLong();

        when(babyAnalysisAgent.analyze(any(BabyAnalysisRequest.class))).thenReturn(
                new BabyAnalysisResult("AI BEFORE DELETE", "mock-model", "mock-agent", "mock-session"),
                new BabyAnalysisResult("AI OK", "mock-model", "mock-agent", "mock-session")
        );

        mockMvc.perform(withBaby(withToken(post("/api/smart-analysis/generate"), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.analysisText").value("AI BEFORE DELETE"));

        mockMvc.perform(withBaby(withToken(delete("/api/records/{id}", deletedFeedId), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"));

        mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", date), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.feeds.length()").value(1))
                .andExpect(jsonPath("$.data.feeds[0].amount").value(80));

        mockMvc.perform(withBaby(withToken(get("/api/records/today/stats"), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.feedCount").value(1))
                .andExpect(jsonPath("$.data.totalMilk").value(80));

        mockMvc.perform(withBaby(withToken(get("/api/records/stats/date").param("date", date), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.recordCount").value(1))
                .andExpect(jsonPath("$.data.feed.count").value(1))
                .andExpect(jsonPath("$.data.feed.totalMilk").value(80));

        mockMvc.perform(withBaby(withToken(get("/api/records/stats/range")
                        .param("startDate", date)
                        .param("endDate", date), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data[0].recordCount").value(1))
                .andExpect(jsonPath("$.data[0].feed.totalMilk").value(80));

        mockMvc.perform(withBaby(withToken(post("/api/smart-analysis/generate"), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.analysisText").value("AI OK"))
                .andExpect(jsonPath("$.data.dataQualityTip").value(containsString("数据较少")));

        ArgumentCaptor<BabyAnalysisRequest> requestCaptor = ArgumentCaptor.forClass(BabyAnalysisRequest.class);
        verify(babyAnalysisAgent, times(2)).analyze(requestCaptor.capture());
        assertEquals(LocalDate.now().minusDays(6), requestCaptor.getAllValues().get(0).getRangeStartDate());
        assertEquals(LocalDate.now(), requestCaptor.getAllValues().get(0).getRangeEndDate());
        String sourceTextBeforeDelete = requestCaptor.getAllValues().get(0).getSourceText();
        String sourceTextAfterDelete = requestCaptor.getAllValues().get(1).getSourceText();
        assertTrue(sourceTextAfterDelete.contains("宝宝所选连续7天记录 TXT"));
        assertTrue(sourceTextAfterDelete.contains("有记录天数: 1/7"));
        assertTrue(sourceTextAfterDelete.contains("数据完整度提示"));
        assertTrue(sourceTextBeforeDelete.contains(deletedMarker));
        assertTrue(sourceTextBeforeDelete.contains("奶瓶/瓶喂总量=280ml"));
        assertTrue(sourceTextAfterDelete.contains("记录总数: 1"));
        assertTrue(sourceTextAfterDelete.contains("奶瓶/瓶喂总量=80ml"));
        assertFalse(sourceTextAfterDelete.contains(deletedMarker));
        assertFalse(sourceTextAfterDelete.contains("200ml"));
    }

    @Test
    void smartAnalysisIncludesPreviousAnalysisComparison() throws Exception {
        String token = loginAndGetToken(uniqueCode("smart-compare"), "Smart Compare Parent");
        Long babyId = createBaby(token, "Smart Compare Baby");
        LocalDate currentEnd = LocalDate.now();
        LocalDate currentStart = currentEnd.minusDays(6);
        LocalDate previousEnd = currentStart.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(6);

        Map<String, Object> previousFeed = new LinkedHashMap<>();
        previousFeed.put("type", "bottle");
        previousFeed.put("amount", 100);
        previousFeed.put("time", previousEnd + " 07:00");
        mockMvc.perform(withBaby(withToken(post("/api/records/feed"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(previousFeed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"));

        Map<String, Object> currentFeed = new LinkedHashMap<>();
        currentFeed.put("type", "bottle");
        currentFeed.put("amount", 160);
        currentFeed.put("time", currentEnd + " 07:00");
        mockMvc.perform(withBaby(withToken(post("/api/records/feed"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(currentFeed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"));

        when(babyAnalysisAgent.analyze(any(BabyAnalysisRequest.class))).thenReturn(
                new BabyAnalysisResult("PREVIOUS WEEK AI", "mock-model", "mock-agent", "mock-session"),
                new BabyAnalysisResult("CURRENT WEEK AI", "mock-model", "mock-agent", "mock-session")
        );

        Map<String, Object> previousRequest = new LinkedHashMap<>();
        previousRequest.put("startDate", previousStart.toString());
        previousRequest.put("endDate", previousEnd.toString());
        mockMvc.perform(withBaby(withToken(post("/api/smart-analysis/generate"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(previousRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.analysisText").value("PREVIOUS WEEK AI"));

        Map<String, Object> currentRequest = new LinkedHashMap<>();
        currentRequest.put("startDate", currentStart.toString());
        currentRequest.put("endDate", currentEnd.toString());
        mockMvc.perform(withBaby(withToken(post("/api/smart-analysis/generate"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(currentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.analysisText").value("CURRENT WEEK AI"));

        ArgumentCaptor<BabyAnalysisRequest> requestCaptor = ArgumentCaptor.forClass(BabyAnalysisRequest.class);
        verify(babyAnalysisAgent, times(2)).analyze(requestCaptor.capture());
        String currentSourceText = requestCaptor.getAllValues().get(1).getSourceText();
        assertTrue(currentSourceText.contains("与上次分析对比"));
        assertTrue(currentSourceText.contains("上一次分析记录"));
        assertTrue(currentSourceText.contains(previousStart + " 至 " + previousEnd));
        assertTrue(currentSourceText.contains("本次=160ml"));
        assertTrue(currentSourceText.contains("上次=100ml"));
        assertTrue(currentSourceText.contains("变化=+60ml"));
        assertTrue(currentSourceText.contains("上一次分析摘要: PREVIOUS WEEK AI"));
    }

    @Test
    void smartAnalysisRejectsNonSevenDayRange() throws Exception {
        String token = loginAndGetToken(uniqueCode("smart-range"), "Smart Range Parent");
        Long babyId = createBaby(token, "Smart Range Baby");
        LocalDate endDate = LocalDate.now();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("startDate", endDate.minusDays(3).toString());
        request.put("endDate", endDate.toString());

        mockMvc.perform(withBaby(withToken(post("/api/smart-analysis/generate"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        verify(babyAnalysisAgent, never()).analyze(any(BabyAnalysisRequest.class));
    }

    @Test
    void customSupplementDoseUnitsPersistWithUserAndCanBeDeleted() throws Exception {
        String token = loginAndGetToken(uniqueCode("custom-dose-unit"), "Dose Unit Parent");
        Long babyId = createBaby(token, "Dose Unit Baby");
        String currentMinute = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        Map<String, Object> supplement = new LinkedHashMap<>();
        supplement.put("type", "supplement");
        supplement.put("name", "Vitamin D");
        supplement.put("dose", "400IU");
        supplement.put("doseUnit", "IU");
        supplement.put("customDoseUnit", true);
        supplement.put("time", currentMinute);

        mockMvc.perform(withBaby(withToken(post("/api/records/supplement"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(supplement)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.dose").value("400IU"))
                .andExpect(jsonPath("$.data.doseUnit").value("IU"))
                .andExpect(jsonPath("$.data.customDoseUnit").value(true));

        mockMvc.perform(withToken(get("/api/records/supplement/custom-items"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.doseUnits[0]").value("IU"));

        mockMvc.perform(withToken(get("/api/records/custom-items").param("type", "dose_unit"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.items[0]").value("IU"));

        mockMvc.perform(withToken(delete("/api/records/custom-items")
                        .param("type", "dose_unit")
                        .param("name", "IU"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"));

        mockMvc.perform(withToken(get("/api/records/supplement/custom-items"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.doseUnits.length()").value(0));
    }

    @Test
    void maternalFoodRecordsPersistWithDailyResponseStatsAndCustomItems() throws Exception {
        String token = loginAndGetToken(uniqueCode("maternal-food"), "Maternal Food Parent");
        Long babyId = createBaby(token, "Maternal Food Baby");
        String date = LocalDate.now().toString();

        Map<String, Object> maternalFood = new LinkedHashMap<>();
        maternalFood.put("mealType", "lunch");
        maternalFood.put("foods", List.of("牛奶/奶制品", "芒果"));
        maternalFood.put("customFoods", List.of("芒果"));
        maternalFood.put("suspicionLevel", "suspect");
        maternalFood.put("note", "观察宝宝腹泻后的反应");
        maternalFood.put("time", date + " 12:10");

        mockMvc.perform(withBaby(withToken(post("/api/records/maternal-food"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maternalFood)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.babyId").value(babyId))
                .andExpect(jsonPath("$.data.mealType").value("lunch"))
                .andExpect(jsonPath("$.data.suspicionLevel").value("suspect"));

        mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", date), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.maternalFoods.length()").value(1))
                .andExpect(jsonPath("$.data.maternalFoods[0].foods[1]").value("芒果"));

        mockMvc.perform(withBaby(withToken(get("/api/records/stats/date").param("date", date), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.maternalFood.count").value(1))
                .andExpect(jsonPath("$.data.maternalFood.suspectCount").value(1));

        mockMvc.perform(withToken(get("/api/records/custom-items").param("type", "maternal_food"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.items[0]").value("芒果"));
    }

    @Test
    void ongoingSleepAcrossMidnightIsSplitIntoDailySegments() throws Exception {
        String token = loginAndGetToken(uniqueCode("sleep-midnight"), "Sleep Parent");
        Long babyId = createBaby(token, "Sleep Baby");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();

        Map<String, Object> sleep = new LinkedHashMap<>();
        sleep.put("startTime", yesterday + " 23:00");

        mockMvc.perform(withBaby(withToken(post("/api/records/sleep"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sleep)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"));

        String latestJson = mockMvc.perform(withBaby(withToken(get("/api/records/sleep/latest"), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode latestSleep = objectMapper.readTree(latestJson).get("data");
        Long todaySleepId = latestSleep.get("id").asLong();
        assertEquals(today + " 00:00", latestSleep.get("startTime").asText());
        assertTrue(latestSleep.get("endTime").isNull());

        String yesterdayJson = mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", yesterday.toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode yesterdaySleep = objectMapper.readTree(yesterdayJson).get("data").get("sleeps").get(0);
        assertEquals(yesterday + " 23:00", yesterdaySleep.get("startTime").asText());
        assertEquals(today + " 00:00", yesterdaySleep.get("endTime").asText());
        assertEquals(60, yesterdaySleep.get("duration").asInt());

        String wakeJson = mockMvc.perform(withBaby(withToken(put("/api/records/sleep/{sleepId}/wake", todaySleepId), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode wokeSleep = objectMapper.readTree(wakeJson).get("data");
        assertEquals(todaySleepId.longValue(), wokeSleep.get("id").asLong());
        assertEquals(today + " 00:00", wokeSleep.get("startTime").asText());
        assertTrue(wokeSleep.get("duration").asInt() >= 1);
    }

    @Test
    void editingCompletedSleepAcrossMidnightSplitsIntoDailySegments() throws Exception {
        String token = loginAndGetToken(uniqueCode("sleep-edit-midnight"), "Sleep Edit Parent");
        Long babyId = createBaby(token, "Sleep Edit Baby");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();

        Map<String, Object> initialSleep = new LinkedHashMap<>();
        initialSleep.put("startTime", yesterday + " 21:00");
        initialSleep.put("endTime", yesterday + " 22:00");
        initialSleep.put("duration", 60);

        String initialJson = mockMvc.perform(withBaby(withToken(post("/api/records/sleep"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialSleep)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long sleepId = objectMapper.readTree(initialJson).get("data").get("id").asLong();

        Map<String, Object> updatedSleep = new LinkedHashMap<>();
        updatedSleep.put("startTime", yesterday + " 23:47");
        updatedSleep.put("endTime", today + " 12:29");
        updatedSleep.put("duration", 762);

        mockMvc.perform(withBaby(withToken(put("/api/records/{id}", sleepId), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedSleep)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"));

        String yesterdayJson = mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", yesterday.toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode yesterdaySleeps = objectMapper.readTree(yesterdayJson).get("data").get("sleeps");
        assertEquals(1, yesterdaySleeps.size());
        assertEquals(sleepId.longValue(), yesterdaySleeps.get(0).get("id").asLong());
        assertEquals(yesterday + " 23:47", yesterdaySleeps.get(0).get("startTime").asText());
        assertEquals(today + " 00:00", yesterdaySleeps.get(0).get("endTime").asText());
        assertEquals(13, yesterdaySleeps.get(0).get("duration").asInt());

        String todayJson = mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", today.toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode todaySleeps = objectMapper.readTree(todayJson).get("data").get("sleeps");
        assertEquals(1, todaySleeps.size());
        Long todaySleepId = todaySleeps.get(0).get("id").asLong();
        assertEquals(today + " 00:00", todaySleeps.get(0).get("startTime").asText());
        assertEquals(today + " 12:29", todaySleeps.get(0).get("endTime").asText());
        assertEquals(749, todaySleeps.get(0).get("duration").asInt());

        Map<String, Object> updatedTodaySegmentEndOnly = new LinkedHashMap<>();
        updatedTodaySegmentEndOnly.put("startTime", today + " 00:00");
        updatedTodaySegmentEndOnly.put("endTime", today + " 12:35");
        updatedTodaySegmentEndOnly.put("duration", 755);

        mockMvc.perform(withBaby(withToken(put("/api/records/{id}", todaySleepId), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedTodaySegmentEndOnly)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"));

        String yesterdayAfterSecondEditJson = mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", yesterday.toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode yesterdayAfterSecondEditSleeps = objectMapper.readTree(yesterdayAfterSecondEditJson).get("data").get("sleeps");
        assertEquals(1, yesterdayAfterSecondEditSleeps.size());
        assertEquals(yesterday + " 23:47", yesterdayAfterSecondEditSleeps.get(0).get("startTime").asText());
        assertEquals(today + " 00:00", yesterdayAfterSecondEditSleeps.get(0).get("endTime").asText());

        String todayAfterSecondEditJson = mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", today.toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode todayAfterSecondEditSleeps = objectMapper.readTree(todayAfterSecondEditJson).get("data").get("sleeps");
        assertEquals(1, todayAfterSecondEditSleeps.size());
        assertEquals(todaySleepId.longValue(), todayAfterSecondEditSleeps.get(0).get("id").asLong());
        assertEquals(today + " 00:00", todayAfterSecondEditSleeps.get(0).get("startTime").asText());
        assertEquals(today + " 12:35", todayAfterSecondEditSleeps.get(0).get("endTime").asText());

        Map<String, Object> updatedTodaySegmentWithWholeStart = new LinkedHashMap<>();
        updatedTodaySegmentWithWholeStart.put("startTime", yesterday + " 23:40");
        updatedTodaySegmentWithWholeStart.put("endTime", today + " 12:40");
        updatedTodaySegmentWithWholeStart.put("duration", 780);

        mockMvc.perform(withBaby(withToken(put("/api/records/{id}", todaySleepId), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedTodaySegmentWithWholeStart)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"));

        String yesterdayAfterThirdEditJson = mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", yesterday.toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode yesterdayAfterThirdEditSleeps = objectMapper.readTree(yesterdayAfterThirdEditJson).get("data").get("sleeps");
        assertEquals(1, yesterdayAfterThirdEditSleeps.size());
        assertEquals(yesterday + " 23:40", yesterdayAfterThirdEditSleeps.get(0).get("startTime").asText());
        assertEquals(today + " 00:00", yesterdayAfterThirdEditSleeps.get(0).get("endTime").asText());

        String todayAfterThirdEditJson = mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", today.toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode todayAfterThirdEditSleeps = objectMapper.readTree(todayAfterThirdEditJson).get("data").get("sleeps");
        assertEquals(1, todayAfterThirdEditSleeps.size());
        assertEquals(todaySleepId.longValue(), todayAfterThirdEditSleeps.get(0).get("id").asLong());
        assertEquals(today + " 00:00", todayAfterThirdEditSleeps.get(0).get("startTime").asText());
        assertEquals(today + " 12:40", todayAfterThirdEditSleeps.get(0).get("endTime").asText());
    }

    @Test
    void deletingCrossDaySleepDeletesAllLinkedSegments() throws Exception {
        String token = loginAndGetToken(uniqueCode("sleep-delete-midnight"), "Sleep Delete Parent");
        Long babyId = createBaby(token, "Sleep Delete Baby");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();

        Map<String, Object> crossDaySleep = new LinkedHashMap<>();
        crossDaySleep.put("startTime", yesterday + " 23:00");
        crossDaySleep.put("endTime", today + " 08:00");
        crossDaySleep.put("duration", 540);

        mockMvc.perform(withBaby(withToken(post("/api/records/sleep"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossDaySleep)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"));

        String todayJson = mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", today.toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode todaySleeps = objectMapper.readTree(todayJson).get("data").get("sleeps");
        assertEquals(1, todaySleeps.size());
        Long todaySleepId = todaySleeps.get(0).get("id").asLong();

        String yesterdayJson = mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", yesterday.toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode yesterdaySleeps = objectMapper.readTree(yesterdayJson).get("data").get("sleeps");
        assertEquals(1, yesterdaySleeps.size());

        mockMvc.perform(withBaby(withToken(delete("/api/records/{id}", todaySleepId), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"));

        mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", today.toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sleeps.length()").value(0));

        mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", yesterday.toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sleeps.length()").value(0));
    }

    @Test
    void readingExistingCrossDaySleepRepairsItIntoDailySegments() throws Exception {
        String token = loginAndGetToken(uniqueCode("sleep-repair-midnight"), "Sleep Repair Parent");
        Long babyId = createBaby(token, "Sleep Repair Baby");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();

        Map<String, Object> badSleep = new LinkedHashMap<>();
        badSleep.put("startTime", yesterday + " 23:47");
        badSleep.put("endTime", today + " 12:29");
        badSleep.put("duration", 762);

        LocalDateTime now = LocalDateTime.now();
        ChildRecord record = new ChildRecord();
        record.setUserId(0L);
        record.setBabyId(babyId);
        record.setRecordType("sleep");
        record.setRecordDate(yesterday);
        record.setRecordTime(LocalDateTime.of(yesterday, LocalTime.of(23, 47)));
        record.setDurationSec(762 * 60);
        record.setPayloadJson(objectMapper.writeValueAsString(badSleep));
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        childRecordMapper.insert(record);

        String todayJson = mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", today.toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode todaySleeps = objectMapper.readTree(todayJson).get("data").get("sleeps");
        assertEquals(1, todaySleeps.size());
        assertEquals(today + " 00:00", todaySleeps.get(0).get("startTime").asText());
        assertEquals(today + " 12:29", todaySleeps.get(0).get("endTime").asText());

        String yesterdayJson = mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", yesterday.toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode yesterdaySleeps = objectMapper.readTree(yesterdayJson).get("data").get("sleeps");
        assertEquals(1, yesterdaySleeps.size());
        assertEquals(yesterday + " 23:47", yesterdaySleeps.get(0).get("startTime").asText());
        assertEquals(today + " 00:00", yesterdaySleeps.get(0).get("endTime").asText());
    }

    @Test
    void userDataIsIsolatedByToken() throws Exception {
        String firstToken = loginAndGetToken(uniqueCode("user-a"), "User A");
        String secondToken = loginAndGetToken(uniqueCode("user-b"), "User B");
        String date = LocalDate.now().toString();
        Long firstBabyId = createBaby(firstToken, "First User Baby");

        mockMvc.perform(withToken(get("/api/baby/current"), firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"));

        Map<String, Object> secondBaby = new LinkedHashMap<>();
        secondBaby.put("name", "Second User Baby");
        secondBaby.put("gender", "girl");
        secondBaby.put("birthDate", "2026-05-01");

        String secondBabyJson = mockMvc.perform(withToken(post("/api/baby/add"), secondToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondBaby)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.name").value("Second User Baby"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long secondBabyId = objectMapper.readTree(secondBabyJson).get("data").get("id").asLong();

        Map<String, Object> crossUserBabyUpdate = new LinkedHashMap<>();
        crossUserBabyUpdate.put("id", secondBabyId);
        crossUserBabyUpdate.put("name", "Cross User Update");

        mockMvc.perform(withToken(put("/api/baby/update"), firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossUserBabyUpdate)))
                .andExpect(status().isNotFound());

        mockMvc.perform(withToken(get("/api/baby/list"), firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.length()").value(1));

        Map<String, Object> feed = new LinkedHashMap<>();
        feed.put("type", "bottle");
        feed.put("amount", 90);
        feed.put("time", date + " 07:30");

        mockMvc.perform(withBaby(withToken(post("/api/records/feed"), secondToken), secondBabyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"));

        Map<String, Object> sleep = new LinkedHashMap<>();
        sleep.put("startTime", date + " 08:00");

        String sleepJson = mockMvc.perform(withBaby(withToken(post("/api/records/sleep"), secondToken), secondBabyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sleep)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long secondSleepId = objectMapper.readTree(sleepJson).get("data").get("id").asLong();

        mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", date), firstToken), firstBabyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.feeds.length()").value(0))
                .andExpect(jsonPath("$.data.sleeps.length()").value(0));

        mockMvc.perform(withBaby(withToken(put("/api/records/sleep/{sleepId}/wake", secondSleepId), firstToken), firstBabyId))
                .andExpect(status().isNotFound());
    }

    @Test
    void recordsAreIsolatedByBabyIdForSameUser() throws Exception {
        String token = loginAndGetToken(uniqueCode("twins"), "Twins Parent");
        Long firstBabyId = createBaby(token, "Twin A");
        Long secondBabyId = createBaby(token, "Twin B");
        String date = LocalDate.now().toString();

        Map<String, Object> feed = new LinkedHashMap<>();
        feed.put("type", "bottle");
        feed.put("amount", 80);
        feed.put("time", date + " 07:10");

        mockMvc.perform(withBaby(withToken(post("/api/records/feed"), token), firstBabyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.babyId").value(firstBabyId));

        Map<String, Object> sleep = new LinkedHashMap<>();
        sleep.put("startTime", date + " 08:00");

        String secondSleepJson = mockMvc.perform(withBaby(withToken(post("/api/records/sleep"), token), secondBabyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sleep)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.babyId").value(secondBabyId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long secondSleepId = objectMapper.readTree(secondSleepJson).get("data").get("id").asLong();

        mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", date), token), firstBabyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.feeds.length()").value(1))
                .andExpect(jsonPath("$.data.sleeps.length()").value(0));

        mockMvc.perform(withBaby(withToken(get("/api/records/date").param("date", date), token), secondBabyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.feeds.length()").value(0))
                .andExpect(jsonPath("$.data.sleeps.length()").value(1));

        mockMvc.perform(withBaby(withToken(put("/api/records/sleep/{sleepId}/wake", secondSleepId), token), firstBabyId))
                .andExpect(status().isNotFound());
    }

    @Test
    void feverTrackingReopensAfterRecoveryAndReturnsLatestMedicineTime() throws Exception {
        String token = loginAndGetToken(uniqueCode("fever-tracking"), "Fever Parent");
        Long babyId = createBaby(token, "Fever Baby");
        String currentMinute = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        Map<String, Object> temperature = new LinkedHashMap<>();
        temperature.put("temperature", 38.2);
        temperature.put("isAbnormal", false);
        temperature.put("time", currentMinute);

        mockMvc.perform(withBaby(withToken(post("/api/records/temperature"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(temperature)))
                .andExpect(status().isOk());

        mockMvc.perform(withBaby(withToken(get("/api/records/today/stats"), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasFever").value(true))
                .andExpect(jsonPath("$.data.feverInfo.temperature").value(38.2));

        mockMvc.perform(withBaby(withToken(post("/api/records/fever-resolved"), token), babyId))
                .andExpect(status().isOk());

        mockMvc.perform(withBaby(withToken(get("/api/records/today/stats"), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasFever").value(false));

        mockMvc.perform(withBaby(withToken(get("/api/records/date")
                        .param("date", LocalDate.now().toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.temperatures[0].resolved").value(true))
                .andExpect(jsonPath("$.data.temperatures[0].resolvedTime").exists());

        temperature.put("temperature", 38.8);
        mockMvc.perform(withBaby(withToken(post("/api/records/temperature"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(temperature)))
                .andExpect(status().isOk());

        Map<String, Object> medicine = new LinkedHashMap<>();
        medicine.put("type", "medicine");
        medicine.put("name", "布洛芬");
        medicine.put("dose", "2ml");
        medicine.put("time", currentMinute);
        mockMvc.perform(withBaby(withToken(post("/api/records/supplement"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(medicine)))
                .andExpect(status().isOk());

        mockMvc.perform(withBaby(withToken(get("/api/records/today/stats"), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasFever").value(true))
                .andExpect(jsonPath("$.data.feverInfo.temperature").value(38.8))
                .andExpect(jsonPath("$.data.lastMedicineTime").value(currentMinute));

        mockMvc.perform(withBaby(withToken(get("/api/records/stats/date")
                        .param("date", LocalDate.now().toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.abnormal.feverCount").value(2));

        mockMvc.perform(withBaby(withToken(get("/api/records/date")
                        .param("date", LocalDate.now().toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.temperatures[0].resolved").value(true))
                .andExpect(jsonPath("$.data.temperatures[1].resolved").value(false));
    }

    @Test
    void diarrheaTrackingContinuesAcrossDaysAndReopensAfterRecovery() throws Exception {
        String token = loginAndGetToken(uniqueCode("diarrhea-tracking"), "Diarrhea Parent");
        Long babyId = createBaby(token, "Diarrhea Baby");

        Map<String, Object> diaper = new LinkedHashMap<>();
        diaper.put("type", "dirty");
        diaper.put("diarrhea", java.util.List.of("水样便"));
        diaper.put("abnormal", true);
        diaper.put("time", LocalDate.now().minusDays(1) + " 22:00");

        mockMvc.perform(withBaby(withToken(post("/api/records/diaper"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(diaper)))
                .andExpect(status().isOk());

        mockMvc.perform(withBaby(withToken(get("/api/records/today/stats"), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasDiarrhea").value(true))
                .andExpect(jsonPath("$.data.diarrheaTypes").value("水样便"));

        mockMvc.perform(withBaby(withToken(post("/api/records/diarrhea-resolved"), token), babyId))
                .andExpect(status().isOk());

        mockMvc.perform(withBaby(withToken(get("/api/records/today/stats"), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasDiarrhea").value(false));

        mockMvc.perform(withBaby(withToken(get("/api/records/date")
                        .param("date", LocalDate.now().minusDays(1).toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.diapers[0].resolved").value(true))
                .andExpect(jsonPath("$.data.diapers[0].resolvedTime").exists());

        diaper.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        mockMvc.perform(withBaby(withToken(post("/api/records/diaper"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(diaper)))
                .andExpect(status().isOk());

        mockMvc.perform(withBaby(withToken(get("/api/records/today/stats"), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasDiarrhea").value(true));

        mockMvc.perform(withBaby(withToken(get("/api/records/stats/date")
                        .param("date", LocalDate.now().toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.abnormal.diarrheaCount").value(1));

        mockMvc.perform(withBaby(withToken(get("/api/records/date")
                        .param("date", LocalDate.now().toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.diapers[0].resolved").value(false));
    }

    @Test
    void otherAbnormalTrackingCanBeResolvedAndReopened() throws Exception {
        String token = loginAndGetToken(uniqueCode("abnormal-tracking"), "Abnormal Parent");
        Long babyId = createBaby(token, "Abnormal Baby");
        String currentMinute = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        Map<String, Object> abnormal = new LinkedHashMap<>();
        abnormal.put("other", "精神状态较差");
        abnormal.put("time", currentMinute);

        mockMvc.perform(withBaby(withToken(post("/api/records/abnormal"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(abnormal)))
                .andExpect(status().isOk());

        mockMvc.perform(withBaby(withToken(get("/api/records/today/stats"), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasOtherAbnormal").value(true))
                .andExpect(jsonPath("$.data.otherAbnormalInfo.other").value("精神状态较差"));

        mockMvc.perform(withBaby(withToken(post("/api/records/abnormal-resolved"), token), babyId))
                .andExpect(status().isOk());

        mockMvc.perform(withBaby(withToken(get("/api/records/today/stats"), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasOtherAbnormal").value(false));

        mockMvc.perform(withBaby(withToken(get("/api/records/date")
                        .param("date", LocalDate.now().toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.abnormals[0].resolved").value(true))
                .andExpect(jsonPath("$.data.abnormals[0].resolvedTime").exists());

        abnormal.put("other", "出现皮疹");
        mockMvc.perform(withBaby(withToken(post("/api/records/abnormal"), token), babyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(abnormal)))
                .andExpect(status().isOk());

        mockMvc.perform(withBaby(withToken(get("/api/records/today/stats"), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasOtherAbnormal").value(true))
                .andExpect(jsonPath("$.data.otherAbnormalInfo.other").value("出现皮疹"));

        mockMvc.perform(withBaby(withToken(get("/api/records/date")
                        .param("date", LocalDate.now().toString()), token), babyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.abnormals[0].resolved").value(true))
                .andExpect(jsonPath("$.data.abnormals[1].resolved").value(false));
    }

    private String loginAndGetToken() throws Exception {
        return loginAndGetToken(uniqueCode("mock-code"), "Test Mom");
    }

    private Long createBaby(String token, String name) throws Exception {
        Map<String, Object> create = new LinkedHashMap<>();
        create.put("name", name);
        create.put("gender", "girl");
        create.put("birthDate", "2026-05-01");

        String createJson = mockMvc.perform(withToken(post("/api/baby/add"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(createJson).get("data").get("id").asLong();
    }

    private String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private Map<String, Object> loginPayload(String code, String nickName) {
        return loginPayload(code, nickName, "https://example.com/avatar.png");
    }

    private Map<String, Object> loginPayload(String code, String nickName, String avatarUrl) {
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("nickName", nickName);
        userInfo.put("avatarUrl", avatarUrl);
        userInfo.put("gender", 2);

        Map<String, Object> login = new LinkedHashMap<>();
        login.put("code", code);
        login.put("userInfo", userInfo);
        return login;
    }

    private String loginAndGetToken(String code, String nickName) throws Exception {
        String loginJson = mockMvc.perform(post("/api/auth/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload(code, nickName))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.userInfo.nickName").value(nickName))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(loginJson).get("data").get("token").asText();
    }

    private String getShareReferrerId(String token) throws Exception {
        String dashboardJson = mockMvc.perform(withToken(get("/api/points/dashboard"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("000000"))
                .andExpect(jsonPath("$.data.shareReferrerId", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(dashboardJson).get("data").get("shareReferrerId").asText();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withToken(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withBaby(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            Long babyId) {
        return request.header("X-Baby-Id", String.valueOf(babyId));
    }
}
