package gov.nist.asbestos.client.events;

import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;

public interface ITask {
    public static final String HTTP_VERB_GET = "GET";
    public static final String HTTP_VERB_POST = "POST";
    public static final String HTTP_VERB_DELETE = "DELETE";

    String getVerb();

    Event getEvent();

    ITask newTask();

    boolean hasRun();

    void fromTask(HttpBase base);

    void putRequestHeader(Headers headers);

    void putRequestBody(byte[] body);

    byte[] getRequestBody();

    Headers getRequestHeader();

    String getRequestBodyAsString();

    void putResponseHeader(Headers headers);

    void putResponseBody(byte[] body);

    void putResponseBodyText(String body);

    void putDescription(String description);

    void putRequestBodyText(String body);

    void putResponseHTMLBody(byte[] body);

    void putRequestHTMLBody(byte[] body);

    Headers getResponseHeader();

    byte[] getResponseBody();

    String getDescription();

    String getResponseBodyAsString();

    HttpBase getHttpBase();
}
