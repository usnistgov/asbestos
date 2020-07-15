package gov.nist.asbestos.proxyWar;

import groovy.transform.SelfType;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JerseyITx {
    private static WebTarget target;

    @BeforeAll
    static void setup() {
        Client c = ClientBuilder.newClient();
        target = c.target("http://localhost:8085/xdstools/");
    }

    @Test
    void getCodes() {
        String responseMsg = target.path("sim/codes/default").request().get(String.class);
        assertTrue(responseMsg.startsWith("<?xml "));
    }

    @Test
    void getasbtsrrConfig() {
        try {
            String responseMsg = target.path("rest/simulators/default__asbtsrr").request().get(String.class);
            assertTrue(responseMsg.startsWith("<?xml "));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
