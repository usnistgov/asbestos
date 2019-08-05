package gov.nist.asbestos.client.resolver;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SearchParms {
    private String parms = null;

    public void setParms(String parms) throws UnsupportedEncodingException {
        setParms(parms, true);
    }

    public void setParms(String parms, boolean encode) throws UnsupportedEncodingException {
        if (encode) {
            boolean startWithQuestion = false;
            if (parms.startsWith("?")) {
                startWithQuestion = true;
                parms = parms.substring(1);
            }
            this.parms = URLEncoder.encode(parms, StandardCharsets.UTF_8.toString());
            if (startWithQuestion)
                this.parms = "?" + this.parms;
        } else {
            this.parms = parms;
        }
    }

    public String getParms() {
        if (parms == null)
            return "";
        if (parms.equals(""))
            return parms;
        if (parms.startsWith("?"))
            return parms;
        return "?" + parms;
    }

    public boolean isSearch() {
        return parms != null && parms.contains("?");
    }
}
