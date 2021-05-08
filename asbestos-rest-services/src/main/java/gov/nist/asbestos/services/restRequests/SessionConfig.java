package gov.nist.asbestos.services.restRequests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SessionConfig {
    private String name;
    private List<String> includes = new ArrayList<>();
    private boolean sessionConfigLocked;


    public SessionConfig(String name, String[] includes, boolean sessionConfigLocked) {
        this.name = name;
        if (includes != null && includes.length > 0) {
            this.includes.addAll(Arrays.asList(includes));
        }
        this.sessionConfigLocked = sessionConfigLocked;
    }

    public String getName() {
        return name;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public boolean isSessionConfigLocked() {
        return sessionConfigLocked;
    }

    /**
     * To be used only by ObjectMapper
     */
    public SessionConfig() {
    }

    /**
     * To be used only by ObjectMapper
     */

    public void setName(String name) {
        this.name = name;
    }

    /**
     * To be used only by ObjectMapper
     */
    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    /**
     * To be used only by ObjectMapper
     */
    public void setSessionConfigLocked(boolean sessionConfigLocked) {
        this.sessionConfigLocked = sessionConfigLocked;
    }
}
