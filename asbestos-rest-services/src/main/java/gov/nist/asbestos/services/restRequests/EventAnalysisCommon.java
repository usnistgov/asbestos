package gov.nist.asbestos.services.restRequests;

import com.google.gson.Gson;
import gov.nist.asbestos.analysis.AnalysisReport;
import gov.nist.asbestos.analysis.RelatedReport;
import gov.nist.asbestos.analysis.Report;
import gov.nist.asbestos.client.Base.EventContext;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.operations.HttpGetter;
import org.hl7.fhir.r4.model.BaseResource;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventAnalysisCommon {
    static void runAndReturnReport(ResourceWrapper wrapper, Request request, EventContext eventContext, ResourceWrapper contextBundle) throws IOException {
        Report report;
        try {
            BaseResource resource = wrapper.getResource();  // might force pulling of resource from logs
            if (resource == null && !wrapper.hasHttpBase()) {
                HttpGetter getter = new HttpGetter();
                String url = wrapper.hasFocusUrl() ? wrapper.getFocusUrl() :  wrapper.getRef().getUri().toString();
                getter.get(new URI(url), Format.JSON.getContentType());
                wrapper.setHttpBase(getter);
            }
            report = new AnalysisReport(request.ec, wrapper)
                    .withContextResource(contextBundle)
                    .run();
        } catch (Throwable t) {
            report = new Report(t.getMessage());
            returnReport(report, request, eventContext);
            return;
        }
        returnReport(report, request, eventContext);
    }

    static void returnReport(Report report, Request request, EventContext eventContext) throws IOException {
        Objects.requireNonNull(report);
        if (!report.hasErrors()) {
            if (report.getBase() != null) {
                report.getBase().setEventContext(eventContext);
                for (RelatedReport rr : report.getObjects()) {
                    rr.setEventContext(eventContext);
                }
            }
        }
        String json = new Gson().toJson(report);
        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print(json);
        request.ok();
    }

    static void runAndReturnReport(Request request, Ref ref, String source, EventAnalysisParams eventAnalysisParams, ResourceWrapper contextBundle) throws IOException {
        AnalysisReport analysisReport = new AnalysisReport(ref, source, request.ec)
                .withGzip(eventAnalysisParams.isGzip())
                .withProxy(eventAnalysisParams.isUseProxy())
                .withValidation(eventAnalysisParams.isValidation())
                .withContextResource(contextBundle);

        Report report;
        try {
            report = analysisReport.run();
        } catch (Throwable t) {
            report = new Report(t.getMessage());
            returnReport(report, request, null);
            return;
        }

        if (eventAnalysisParams.isIgnoreBadRefs()) {
            List<String> errors = report.getErrors();
            List<String> newErrors = new ArrayList<>();
            for (String error : errors) {
                if (error.startsWith("Cannot load")) {
                    String[] words = error.split(" ");
                    String url = words[2];
                    if (url.startsWith("http")) {
                        Ref ref1 = new Ref(url);
                        if (!ref1.hasResource())
                            continue;
                    }
                }
                newErrors.add(error);
            }
            report.setErrors(newErrors);
        }

        returnReport(report, request, null);
    }

    static void runAndReturnReport(Request request, Ref ref, String source, EventAnalysisParams eventAnalysisParams, ResourceWrapper contextBundle, EventContext eventContext) throws IOException {
        AnalysisReport analysisReport = new AnalysisReport(ref, source, request.ec)
                .withGzip(eventAnalysisParams.isGzip())
                .withProxy(eventAnalysisParams.isUseProxy())
                .withValidation(eventAnalysisParams.isValidation())
                .withContextResource(contextBundle);

        Report report;
        try {
            report = analysisReport.run();
        } catch (Throwable t) {
            report = new Report(t.getMessage());
            returnReport(report, request, eventContext);
            return;
        }

        if (eventAnalysisParams.isIgnoreBadRefs()) {
            List<String> errors = report.getErrors();
            List<String> newErrors = new ArrayList<>();
            for (String error : errors) {
                if (error.startsWith("Cannot load")) {
                    String[] words = error.split(" ");
                    String url = words[2];
                    if (url.startsWith("http")) {
                        Ref ref1 = new Ref(url);
                        if (!ref1.hasResource())
                            continue;
                    }
                }
                newErrors.add(error);
            }
            report.setErrors(newErrors);
        }

        returnReport(report, request, eventContext);
    }

}
