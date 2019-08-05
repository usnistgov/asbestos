package gov.nist.asbestos.mhd.translation.test;

import gov.nist.asbestos.mhd.translation.attribute.Id;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IdTest {

    @Test
    void simpleId() {
        Id id = new Id("4");
        assertEquals("4", id.getId());
        assertNull(id.getAssigningAuthority());
        assertNull(id.getAaOid());
        assertNull(id.getTypeCode());
        assertEquals("4", id.toString());
    }

    @Test
    void idAA() {
        Id id = new Id("4^^^&1.2.3&ISO");
        assertEquals("4", id.getId());
        assertEquals("&1.2.3&ISO", id.getAssigningAuthority());
        assertEquals("1.2.3", id.getAaOid());
        assertNull(id.getTypeCode());
        assertEquals("4^^^&1.2.3&ISO", id.toString());
    }

    @Test
    void idAATypeCode() {
        Id id = new Id("4^^^&1.2.3&ISO^urn:ihe:iti:xds:2013:uniqueId");
        assertEquals("4", id.getId());
        assertEquals("&1.2.3&ISO", id.getAssigningAuthority());
        assertEquals("1.2.3", id.getAaOid());
        assertEquals("urn:ihe:iti:xds:2013:uniqueId", id.getTypeCode());
        assertEquals("4^^^&1.2.3&ISO^urn:ihe:iti:xds:2013:uniqueId", id.toString());
    }

    @Test
    void idFromIdAndAA() {
        Id id = new Id();
        id.setId("4");
        id.setAaOid("1.2.3.4");
        assertEquals("4^^^&1.2.3.4&ISO", id.toString());
    }

    @Test
    void idFromIdAndAAAndTypeCode() {
        Id id = new Id();
        id.setId("4");
        id.setAaOid("1.2.3.4");
        id.setTypeCode("urn:foo");
        assertEquals("4^^^&1.2.3.4&ISO^urn:foo", id.toString());
    }
}
