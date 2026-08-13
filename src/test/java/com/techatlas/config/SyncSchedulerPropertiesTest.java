package com.techatlas.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SyncSchedulerPropertiesTest {

    @Test
    void testDefaultValues() {
        SyncSchedulerProperties properties = new SyncSchedulerProperties();
        assertTrue(properties.isEnabled());
        assertEquals(3600000L, properties.getFixedDelayMs());
        assertEquals(30000L, properties.getInitialDelayMs());
        assertTrue(properties.getWikipedia().isEnabled());
        assertTrue(properties.getGithub().isEnabled());
        assertTrue(properties.getStackoverflow().isEnabled());
    }

    @Test
    void testMutations() {
        SyncSchedulerProperties properties = new SyncSchedulerProperties();
        properties.setEnabled(false);
        properties.setFixedDelayMs(10000L);
        properties.setInitialDelayMs(5000L);
        properties.getWikipedia().setEnabled(false);

        assertFalse(properties.isEnabled());
        assertEquals(10000L, properties.getFixedDelayMs());
        assertEquals(5000L, properties.getInitialDelayMs());
        assertFalse(properties.getWikipedia().isEnabled());
    }
}
