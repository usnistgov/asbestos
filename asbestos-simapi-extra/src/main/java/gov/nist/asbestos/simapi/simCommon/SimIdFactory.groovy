package gov.nist.asbestos.simapi.simCommon;

import groovy.transform.TypeChecked;

@TypeChecked
 class SimIdFactory implements Serializable {
     SimIdFactory() {
    }

    static  SimId simIdBuilder(String rawId) {
        String[] parts = rawId.split("__");
        assert parts.length == 2 : "Not a valid SimId - " + rawId
        return new SimId(new TestSession(parts[0]), parts[1]);
    }

    static  SimId simIdBuilder(TestSession testSession, String id) {
        assert !id.contains("__") : "Cannot construct a SimId from " + testSession + " and " + id
        return new SimId(testSession, id);
    }

    static  boolean isSimId(String rawId) {
        String[] parts = rawId.split("__");
        if (parts.length != 2) return false;
        return true;
    }
}
