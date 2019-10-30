package gov.nist.asbestos.asbestosProxy.channels.mhd.capabilitystatement;

import org.apache.log4j.Logger;

import java.net.URI;

public class CapabilityStatement {
    private static Logger logger = Logger.getLogger(CapabilityStatement.class);

    public static boolean isCapabilityStatementRequest(URI baseUri, URI requestUri) {
        try {
            // Ignoring the query string, exclusively check for the only segment right after the base path.
            // Should match: base-path/segment
            // But not: base-path/abc/segment/sdf or base-path/segment/abc
            String[] basePath = baseUri.getPath().split("/");
            if (basePath.length > 0) {
                String[] requestPath = requestUri.getPath().split("/");
                if (requestPath.length == basePath.length + 1) {
                    if ("metadata".equals(requestPath[basePath.length])) {
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            logger.info("isCapabilityStatementRequest: " + ex.toString());
        }
        return false;
    }


}
