package gov.nist.asbestos.mhd.translation.search;

import gov.nist.asbestos.mhd.translation.attribute.DateTransform;
import gov.nist.asbestos.mhd.util.Utils;

import java.util.*;

/**
 * generate SQ parameters from MHD query spec.  Inputs and outputs are in a Map-based model.
 */
class DocRefSQParamTranslator {
    // DocumentEntry SQ parameters
    static final String statusKey = "$XDSDocumentEntryStatus";
    static final String patientIdKey = "$XDSDocumentEntryPatientId";
    static final String creationFromKey = "$XDSDocumentEntryCreationTimeFrom";
    static final String creationToKey = "$XDSDocumentEntryCreationTimeTo";
    static final String classKey = "$XDSDocumentEntryClassCode";
    static final String typeKey = "$XDSDocumentEntryTypeCode";
    static final String settingKey = "$XDSDocumentEntryPracticeSettingCode";
    static final String serviceStartFromKey = "$XDSDocumentEntryServiceStartTimeFrom";
    static final String serviceStartToKey = "$XDSDocumentEntryServiceStartTimeTo";
    static final String serviceStopFromKey = "$XDSDocumentEntryServiceStopTimeFrom";
    static final String serviceStopToKey = "$XDSDocumentEntryServiceStopTimeTo";
    static final String facilityKey = "$XDSDocumentEntryHealthcareFacilityTypeCode";
    static final String eventKey = "$XDSDocumentEntryEventCodeList";
    static final String confKey = "$XDSDocumentEntryConfidentialityCode";
    static final String formatKey = "$XDSDocumentEntryFormatCode";
    static final String relatedKey = "$XDSDocumentEntryReferenceIdList";
    static final String authorKey = "$XDSDocumentEntryAuthorPerson";
    static final String entryUUIDKey = "$XDSDocumentEntryEntryUUID";
    static final String uniqueIdKey = "$XDSDocumentEntryUniqueId";
    static final String queryType = "QueryType";

    static String docEntryUniqueId = "$XDSDocumentEntryUniqueId";

    // coded types
    public final static List<String> codedTypes = Arrays.asList(
            classKey,
            typeKey,
            settingKey,
            facilityKey,
            eventKey,
            confKey,
            formatKey,
            relatedKey,
            docEntryUniqueId
    );

    static final List<String> acceptsMultiple = Arrays.asList(
            classKey,
            typeKey,
            settingKey,
            facilityKey,
            eventKey,
            confKey,
            authorKey,
            formatKey,
            statusKey,
            entryUUIDKey
    );

    // Query Types
    static final String FindDocsKey = "urn:uuid:14d4debf-8f97-4251-9a74-a90016b0af0d";
    static final String FindDocsByRefIdKey = "urn:uuid:12941a89-e02e-4be5-967c-ce4bfc8fe492";
    static final String GetDocs = "urn:uuid:5c4f972b-d56b-40ac-a5fc-c8ca9b40b9d4";

    // shows default query - may be upgraded to FindDocsByRefIdKey if related-id is used
    static Map<String, List<String>> result = new HashMap<>();
    static {
        result.put("QueryType", Arrays.asList(FindDocsKey));
    }

    private void addResult(String key, String value) {
        result.put(key, Arrays.asList(value));
    }

    private void addResult(String key,  List<String> value) {
        result.put(key, value);
    }

    // translation table between FHIR and XDS coding styles
    // TODO FHIR code/identifier matching is not case sensitive
    static class CodeTr {
        String fhir;
        String xds;
        CodeTr(String fhir, String xds) {
            this.fhir = fhir;
            this.xds = xds;
        }
    }
    static List<CodeTr> codes = new ArrayList<>();
    static {
        codes.add(new CodeTr("class=urn:class:system|class1", "$XDSDocumentEntryClassCode:class1^^1.2.3.6677"));
        codes.add(new CodeTr("related-id=urn:relatedid:system|relatedid1", "$XDSDocumentEntryReferenceIdList:relatedid1^^1.2.3.6677.7"));
        codes.add(new CodeTr("format=urn:format:system|format1", "$XDSDocumentEntryFormatCode:format1^^1.2.3.6677.6"));
        codes.add(new CodeTr("securityLabel=urn:securityLabel:system|securityLabel1", "$XDSDocumentEntryConfidentialityCode:securityLabel1^^1.2.3.6677.5"));
        codes.add(new CodeTr("event=urn:event:system|event1", "$XDSDocumentEntryEventCodeList:event1^^1.2.3.6677.4"));
        codes.add(new CodeTr("facility=urn:facility:system|facility1", "$XDSDocumentEntryHealthcareFacilityTypeCode:facility1^^1.2.3.6677.3"));
        codes.add(new CodeTr("setting=urn:setting:system|setting1", "$XDSDocumentEntryPracticeSettingCode:setting1^^1.2.3.6677.2"));
        codes.add(new CodeTr("type=urn:type:system|type1", "$XDSDocumentEntryTypeCode:type1^^1.2.3.6677.1"));
    }

    private class Code {
        String type;
        String system;
        String code;
    }

    private class FhirCode extends Code {

        FhirCode(String encoded) {
            String[] typecoded = encoded.split("=", 2);
            type = typecoded[0];
            String coded = typecoded[1];
            String[] systemcode = coded.split("\\|", 2);
            system = systemcode[0];
            code = systemcode[1];
        }

        XdsCode findXds() {
            String coded = asCode();
            CodeTr entry = null;
            for (CodeTr code : codes) {
                if (code.fhir.equals(coded)) {
                    entry = code;
                }
            }
            if (entry != null)
                return new XdsCode(entry.xds);
            return null;
        }

        String asCode() { return type + "=" + system + "|" + code; }

        public String toString() { return asCode(); }
    }

    private class XdsCode extends Code {

        XdsCode(String encoded) {
            String[] typecoded = encoded.split(":", 2);
            type = typecoded[0];
            String coded = typecoded[1];
            String[] codesystem = coded.split("\\^\\^", 2);
            code = codesystem[0];
            system = codesystem[1];
        }

        FhirCode findFhir() {
            String coded = asCode();
            CodeTr entry = null;
            for (CodeTr code : codes) {
                if (code.xds.equals(coded)) {
                    entry = code;
                }
            }
            if (entry != null)
                return new FhirCode(entry.fhir);
            return null;
        }

        String asCode() { return type + ":" + code + "^^" + system; }

        public String toString() { return asCode(); }
    }

    static String cannotTranslateFhir(String param) {
        throw new RuntimeException(String.format("Cannot translate code '%s' to XDS, no mapping defined for this code.", param));
    }

    List<String> addDEStatusIfNotPresent(List<String> params) {
        boolean found = false;
        for (String param : params) {
            if (param.startsWith("status"))
                found = true;
        }
        if (!found)
            params.add("status=current");
        return params;
    }

    Map<String, List<String>> run(List<String> params) {
        params = addDEStatusIfNotPresent(params);
        Map<String, List<String>> result = new HashMap<>();
        for (String param : params) {
            Map<String, List<String>> r = run(param);
            result.putAll(r);
        }
        return result;
    }

    Map<String, List<String>> run(String param) {

        String[] paramParts = param.split("=", 2);
        String name = paramParts[0];
        String value = paramParts[1];

        switch (name) {
            case "patient.identifier":
                String[] systemcode = value.split("\\|", 2);
                String system = systemcode[0];
                String code = systemcode[1];
                if (system != null && system.startsWith("urn:oid:"))
                    system = system.substring("urn:oid:".length());
                addResult(patientIdKey, code + "^^^&" + system + "&ISO");
                break;

            case "status":
                if (value.equals("current"))
                    addResult(statusKey,"urn:oasis:names:tc:ebxml-regrep:StatusType:Approved");
                if (value.equals("superseded"))
                    addResult(statusKey, "urn:oasis:names:tc:ebxml-regrep:StatusType:Deprecated");
                break;

            case "indexed":
                String op = value.substring(0, 2);
                String date = value.substring(2);
                String dtm = DateTransform.fhirToDtm(date);
                addTerms(param, op, creationFromKey, creationToKey, dtm);
                break;

            case "period":
                op = value.substring(0, 2);
                date = value.substring(2);
                dtm = DateTransform.fhirToDtm(date);
                addTerms(param, op, serviceStartFromKey, serviceStopToKey, dtm);
                break;

            case "class":
                FhirCode fcode = new FhirCode(param);
                XdsCode xcode = fcode.findXds();
                if (xcode != null)
                    addResult(classKey, xcode.code + "^^" + xcode.system);
                else
                    cannotTranslateFhir(param);
                break;

            case "type":
                fcode = new FhirCode(param);
                xcode = fcode.findXds();
                if (xcode != null)
                    addResult(typeKey, xcode.code + "^^" + xcode.system);
                else
                    cannotTranslateFhir(param);
                break;

            case "setting":
                fcode = new FhirCode(param);
                xcode = fcode.findXds();
                if (xcode != null)
                    addResult(settingKey, xcode.code + "^^" + xcode.system);
                else
                    cannotTranslateFhir(param);
                break;

            case "facility":
                fcode = new FhirCode(param);
                xcode = fcode.findXds();
                if (xcode != null)
                    addResult(facilityKey, xcode.code + "^^" + xcode.system);
                else
                    cannotTranslateFhir(param);
                break;

            case "event":
                fcode = new FhirCode(param);
                xcode = fcode.findXds();
                if (xcode != null)
                    addResult(eventKey, xcode.code + "^^" + xcode.system);
                else
                    cannotTranslateFhir(param);
                break;

            case "securityLabel":
                fcode = new FhirCode(param);
                xcode = fcode.findXds();
                if (xcode != null)
                    addResult(confKey, xcode.code + "^^" + xcode.system);
                else
                    cannotTranslateFhir(param);
                break;

            case "format":
                fcode = new FhirCode(param);
                xcode = fcode.findXds();
                if (xcode != null)
                    addResult(formatKey, xcode.code + "^^" + xcode.system);
                else
                    cannotTranslateFhir(param);
                break;

            case "related-id":
                fcode = new FhirCode(param);
                xcode = fcode.findXds();
                if (xcode != null) {
                    addResult(relatedKey, xcode.code + "^^" + xcode.system);
                    addResult(queryType, FindDocsByRefIdKey);
                }
                else
                    cannotTranslateFhir(param);
                break;

            case "identifier":
                if (value != null && value.startsWith("urn:uuid:"))
                    addResult(entryUUIDKey, value);
                else
                    addResult(uniqueIdKey, Utils.stripUrnPrefix(value));
                break;

            default:
                throw new RuntimeException("Query parameter " + name + " cannot be translated into Stored Query parameters.");

        }
        return result;
    }

    private void addTerms(String param, String op, String fromKey, String toKey, String dtm) {
        switch (op) {
            case "eq":
            case "ap":  // approximate is in the eys of the beholder
                addResult(fromKey, dtm);
                addResult(toKey, dtm);
                break;

            case "ne":
                // TODO - cannot code this in SQ
                break;

            case "lt":
            case "le":   // TODO - could be better
            case "eb": // same as le I think
                addResult(toKey, dtm);
                break;

            case "gt":
            case "ge":   // TODO - could be better
            case "sa": // I think this is right
                addResult(fromKey, dtm);
                break;
            default:
                throw new RuntimeException(param + " cannot be translated into Stored Query parameters.");
        }
    }
}
