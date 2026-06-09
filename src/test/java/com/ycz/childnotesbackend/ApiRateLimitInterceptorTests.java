package com.ycz.childnotesbackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ycz.childnotesbackend.config.RateLimitProperties;
import com.ycz.childnotesbackend.interceptor.ApiRateLimitInterceptor;
import com.ycz.childnotesbackend.mapper.IpBlacklistMapper;
import com.ycz.childnotesbackend.model.entity.IpBlacklist;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiRateLimitInterceptorTests {

    @Test
    void limitsSameIpAndEndpointThenBlacklists() throws Exception {
        RateLimitProperties properties = new RateLimitProperties();
        IpBlacklistMapper mapper = mock(IpBlacklistMapper.class);
        when(mapper.insert(any(IpBlacklist.class))).thenReturn(1);
        ApiRateLimitInterceptor interceptor = new ApiRateLimitInterceptor(properties, mapper, new ObjectMapper());

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            assertTrue(interceptor.preHandle(request(), response, new Object()));
        }

        MockHttpServletResponse rateLimited = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request(), rateLimited, new Object()));
        assertEquals(429, rateLimited.getStatus());

        for (int i = 0; i < 4; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            assertFalse(interceptor.preHandle(request(), response, new Object()));
            assertEquals(429, response.getStatus());
        }

        MockHttpServletResponse blacklisted = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request(), blacklisted, new Object()));
        assertEquals(403, blacklisted.getStatus());
        verify(mapper, times(1)).insert(any(IpBlacklist.class));

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request(), blocked, new Object()));
        assertEquals(403, blocked.getStatus());
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demo/1");
        request.setRemoteAddr("10.0.0.1");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/demo/{id}");
        return request;
    }
}
