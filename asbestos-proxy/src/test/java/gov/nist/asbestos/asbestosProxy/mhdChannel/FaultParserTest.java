package gov.nist.asbestos.asbestosProxy.mhdChannel;

import gov.nist.asbestos.mhd.transactionSupport.FaultParser;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class FaultParserTest {

    @Test
    void trim1() {
        assertEquals("foo", FaultParser.trim("  foo  "));
    }

    @Test
    void trim2() {
        assertEquals("foo", FaultParser.trim("\nfoo\n\n\n"));
    }

    @Test
    void trim3() {
        assertEquals("foo", FaultParser.trim(" \n  foo  \n  \n  \n  "));
    }

    @Test
    void faultPart() throws URISyntaxException, IOException, SAXException {
        String part =     new String(Files.readAllBytes(Paths.get(getClass().getResource("/mhdChannel/fault/faultPart.txt").toURI())));
        String env = FaultParser.unwrapPart(part);
        String envelope = new String(Files.readAllBytes(Paths.get(getClass().getResource("/mhdChannel/fault/faultEnvelope.txt").toURI())));
        assertEquals(env.trim(), envelope.trim());

        String msg = FaultParser.parse(env);
        assertTrue(msg.startsWith("Header/Format Validation errors reported"));
    }

    @Test
    void nonFaultPart() throws URISyntaxException, IOException, SAXException {
        String part =     new String(Files.readAllBytes(Paths.get(getClass().getResource("/mhdChannel/fault/nonFaultPart.txt").toURI())));
        String env = FaultParser.unwrapPart(part);

        String msg = FaultParser.parse(env);
        assertNull(msg);
    }

    @Test
    void extractRegistryResponse() throws URISyntaxException, IOException, SAXException {
        String part =     new String(Files.readAllBytes(Paths.get(getClass().getResource("/mhdChannel/registryError/responsePart.txt").toURI())));
        String env = FaultParser.unwrapPart(part);

        String regResp = FaultParser.extractRegistryResponse(env);
        assertNotNull(regResp);
        assertTrue(regResp.startsWith("<rs:RegistryResponse"));
    }
}
