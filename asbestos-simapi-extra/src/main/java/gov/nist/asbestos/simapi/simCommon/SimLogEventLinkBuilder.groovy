package gov.nist.asbestos.simapi.simCommon

import groovy.transform.TypeChecked;

@TypeChecked
 class SimLogEventLinkBuilder {

    static  String buildUrl(String toolkitBaseUrl, String simIdString, String actor, String trans, String eventId) {
        String token = buildTokenPath(simIdString, actor, trans, eventId);
        return build(toolkitBaseUrl, token);
    }

    static  String build(String toolkitBaseUrl, String token) {
        String relativeUrl = buildInternal(token); //"#SimMsgViewer:" + token;
        return toolkitBaseUrl + relativeUrl;
    }

    static  String buildToken(String simIdString, String actor, String trans, String eventId) {
        return buildTokenPath(simIdString, actor, trans, eventId);
    }

    static  String buildInternal(String token) {
        return "#SimMsgViewer:" + token;
    }

    static  String buildTokenPath(String simIdString, String actor, String trans, String eventId) {
        return simIdString + "/" + actor + "/" + trans + "/" + eventId;
    }
}
