package gov.nist.asbestos.simapi.validation;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ValidationReportBuilder {

    public static String toJson(ValidationReport report) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ValidationReport fromJson(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, ValidationReport.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
