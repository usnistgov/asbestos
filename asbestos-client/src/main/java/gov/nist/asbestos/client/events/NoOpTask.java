package gov.nist.asbestos.client.events;

import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;

public class NoOpTask implements ITask {

    @Override
    public String getVerb() {
        return null;
    }

    @Override
    public Event getEvent() {
        return null;
    }

    @Override
    public ITask newTask() {
        return null;
    }

    @Override
    public boolean hasRun() {
        return false;
    }

    @Override
    public void fromTask(HttpBase base) {

    }

    @Override
    public void putRequestHeader(Headers headers) {

    }

    @Override
    public void putRequestBody(byte[] body) {

    }

    @Override
    public byte[] getRequestBody() {
        return new byte[0];
    }

    @Override
    public Headers getRequestHeader() {
        return null;
    }

    @Override
    public String getRequestBodyAsString() {
        return null;
    }

    @Override
    public void putResponseHeader(Headers headers) {

    }

    @Override
    public void putResponseBody(byte[] body) {

    }

    @Override
    public void putResponseBodyText(String body) {

    }

    @Override
    public void putDescription(String description) {

    }

    @Override
    public void putRequestBodyText(String body) {

    }

    @Override
    public void putResponseHTMLBody(byte[] body) {

    }

    @Override
    public void putRequestHTMLBody(byte[] body) {

    }

    @Override
    public Headers getResponseHeader() {
        return null;
    }

    @Override
    public byte[] getResponseBody() {
        return new byte[0];
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getResponseBodyAsString() {
        return null;
    }
}
