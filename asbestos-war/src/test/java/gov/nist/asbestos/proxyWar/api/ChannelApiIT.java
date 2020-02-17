package gov.nist.asbestos.proxyWar.api;


import gov.nist.asbestos.asbestosApi.Channel;
import gov.nist.asbestos.asbestosApi.impl.ApiConfig;
import gov.nist.asbestos.asbestosApi.impl.ChannelRepo;
import gov.nist.asbestos.proxyWar.support.ITConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChannelApiIT {

    @Test
    void loadDefaultChannel() throws Exception {
        ApiConfig.setAsbestosBase(ITConfig.getAsbestosBase());

        Channel defaultChannel = ChannelRepo.get("default", "default");
        assertNotNull(defaultChannel);
        assertEquals("default", defaultChannel.getChannelId());
        assertEquals("fhir", defaultChannel.getChannelType());
        assertEquals("default", defaultChannel.getEnvironment());
    }

    @Test
    void loadUnknownChannel() throws Exception {
        ApiConfig.setAsbestosBase(ITConfig.getAsbestosBase());

        Channel defaultChannel = ChannelRepo.get("default", "unknown");
        assertNull(defaultChannel);
    }

    @Test
    void createDeleteChannel() throws Exception {
        ApiConfig.setAsbestosBase(ITConfig.getAsbestosBase());

        Channel channel = ChannelRepo.create("default", "default", "fhir", "ittest");
        assertNotNull(channel);

        Channel readBack = ChannelRepo.get("default", "ittest");
        assertNotNull(readBack);
        assertEquals(channel, readBack);

        boolean deleted = ChannelRepo.delete(readBack);
        assertTrue(deleted);
    }
}
