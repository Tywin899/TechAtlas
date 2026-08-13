package com.techatlas.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RedisConfigTest {

    @Test
    void testPropertiesBinding() {
        RedisCacheProperties properties = new RedisCacheProperties();
        properties.setEnabled(true);
        properties.setHost("localhost");
        properties.setPort(6379);
        properties.getSearch().setTtlSeconds(300);
        properties.getDocument().setTtlSeconds(600);

        assertTrue(properties.isEnabled());
        assertEquals("localhost", properties.getHost());
        assertEquals(6379, properties.getPort());
        assertEquals(300, properties.getSearch().getTtlSeconds());
        assertEquals(600, properties.getDocument().getTtlSeconds());
    }
}
