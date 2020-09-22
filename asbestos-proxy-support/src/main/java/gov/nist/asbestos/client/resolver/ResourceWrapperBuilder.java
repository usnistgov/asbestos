package gov.nist.asbestos.client.resolver;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.events.UITask;
import gov.nist.asbestos.http.headers.Headers;
import org.hl7.fhir.r4.model.BaseResource;

import java.io.File;

public class ResourceWrapperBuilder {

    public static ResourceWrapper fromUIEvent(File eventDir, int taskIndex, boolean request) {
        UIEvent event = new UIEvent(new EC(new File(""))).fromEventDir(eventDir);
        if (taskIndex >= event.getTaskCount())
            return null;
        UITask task = event.getTask(taskIndex);

        Headers headers = request ? new Headers(task.getRequestHeader()) : new Headers(task.getResponseHeader());
        String bodyString = request ? task.getRequestBody() : task.getResponseBody();

        BaseResource baseResource = ParserBase.parse(bodyString, Format.fromContentType(headers.getContentType().getValue()));
        return new ResourceWrapper(baseResource);
    }
}
