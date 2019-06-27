package gov.nist.asbestos.client.client;

import java.net.URI;
/**
 * this is necessary because URI(String) will not do encoding.  You need
 * the multi-parameter version to get that support
 */
public class EncodeUri {

    public static URI get(String ref) {
        try {
            if (ref.startsWith("#"))
                return new URI(null, "", ref);
            String[] parts = ref.split(":", 2);
            if (parts.length == 1)
                return new URI(null, parts[0], null);
            String[] partx = parts[1].split("#");
            String scheme = parts[0];
            String ssp = partx[0];
            String[] party = ssp.split("#", 2);
            ssp = party[0];
            String fragment = null;
            if (party.length == 2)
                fragment = party[1];
            return new URI(parts[0], parts[1], fragment);
        } catch (Throwable t) {
            throw new Error(t);
        }
    }
}
