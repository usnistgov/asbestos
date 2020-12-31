package gov.nist.asbestos.mhd.translation.search;

import gov.nist.asbestos.client.events.ITask;
import gov.nist.asbestos.mhd.transactionSupport.AhqrSender;
import gov.nist.asbestos.mhd.transactionSupport.RetrieveContent;
import gov.nist.asbestos.mhd.transactionSupport.RetrieveSender;
import gov.nist.asbestos.mhd.translation.attribute.Slot;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.ResponseOptionType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.AdhocQueryType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.QueryExpressionType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;

import javax.swing.*;
import java.net.URI;
import java.util.*;

import static gov.nist.asbestos.mhd.translation.search.DocRefSQParamTranslator.queryType;

public class FhirSq {

    public static String IheMhdDocumentationReference = "https://profiles.ihe.net/ITI/TF/Volume1/ch-33.html";
    /**
     *
     * @param query is param1=value1;param2=value2...
     *              Delimiter can be either a ';' or a '&' character
     * @return StoredQuery model
     */
    private static Map<String, List<String>> docRefQueryToSQModel(String query) {
        List<String> delimiters = Arrays.asList("&",";");
        final List<String> params = new ArrayList<>();
        delimiters.stream().forEach(s -> {
            if (params.isEmpty()) { // unparsed state
                if (query.contains(s)) {
                    params.addAll(Arrays.asList(query.split(s)));
                }
            }
        });
        if (params.isEmpty()) {// if true then no delimiters were detected in the query string
            params.add(query);
        }
        return new DocRefSQParamTranslator().run(params);
    }

    public static AhqrSender docRefQuery(String httpQueryString, URI toAddr, ITask task) {
        String sqid;
        Map<String, List<String>> params = docRefQueryToSQModel(httpQueryString);
        Set<String> names = params.keySet();
        /*
        patient.identifier and documentReference status is required as per MHD since it only support FindDocuments and FindDocumentByRef type queries
         Add spec page reference here if the above condition is false
         */
         if (names.contains(DocRefSQParamTranslator.uniqueIdKey)
                && names.contains(DocRefSQParamTranslator.statusKey)
                && names.contains(DocRefSQParamTranslator.patientIdKey)) {
             /*
             status and patientIdKey are not really needed here, though the presence of the parameters required by MHD through FindDocuments.
             Asbestos implements GetDocuments and therefore only uniqueId is used.
             */
             sqid = "urn:uuid:5c4f972b-d56b-40ac-a5fc-c8ca9b40b9d4"; /* GetDocuments */
             return run(params, sqid, toAddr, true, task);
         }
         throw new RuntimeException("DocumentReference required search parameters missing. " + IheMhdDocumentationReference);
    }

    public static AhqrSender docRefQuery(List<String> queryParams, URI toAddr, ITask task) {
        return run(new DocRefSQParamTranslator().run(queryParams), "urn:uuid:14d4debf-8f97-4251-9a74-a90016b0af0d" /* FindDocuments */, toAddr, true, task);
    }

    public static AhqrSender docManQuery(String httpQueryString, URI toAddr, ITask task) {
        Map<String, List<String>> model = new HashMap<>();
        return run(new DocManSQParamTranslator().run(httpQueryString), DocManSQParamTranslator.GetSubmissionSetAndContentsKey, toAddr, true, task);
    }

    public static AhqrSender documentEntryByUidQuery(String uid, URI toAddr, ITask task)  {
        Map<String, List<String>> model = new HashMap<>();
        model.put("$XDSDocumentEntryUniqueId", Collections.singletonList(uid));
        return run(model, "urn:uuid:5c4f972b-d56b-40ac-a5fc-c8ca9b40b9d4", toAddr, true, task);
    }

    public static AhqrSender documentEntryByUUIDQuery(String uuid, URI toAddr, ITask task)  {
        Map<String, List<String>> model = new HashMap<>();
        model.put("$XDSDocumentEntryEntryUUID", Collections.singletonList(uuid));
        return run(model, "urn:uuid:5c4f972b-d56b-40ac-a5fc-c8ca9b40b9d4", toAddr, true, task);
    }

    public static RetrieveContent binaryByUidRetrieve(String docUid, String repUid, URI toAddr, ITask task)  {
        RetrieveDocumentSetRequestType request = new RetrieveDocumentSetRequestType();
        RetrieveDocumentSetRequestType.DocumentRequest docRequest = new RetrieveDocumentSetRequestType.DocumentRequest();
        docRequest.setRepositoryUniqueId(repUid);
        docRequest.setDocumentUniqueId(docUid);
        request.getDocumentRequest().add(docRequest);

        return new RetrieveSender().send(request, toAddr, task);
    }

    public static AhqrSender submissionSetByUidQuery(String uid, URI toAddr, ITask task)  {
        Map<String, List<String>> model = new HashMap<>();
        model.put("$XDSSubmissionSetUniqueId", Collections.singletonList(uid));
        return run(model, "urn:uuid:e8e3cb2c-e39c-46b9-99e4-c12f57260b83", toAddr, true, task);

        // query returns more than what we want - prune results
    }

    // model is [queryParamName: [values]]
    // toAddr is SQ endpoint on registry
    public static AhqrSender run(Map<String, List<String>> theModel, String sqid, URI toAddr, boolean leafClass, ITask task) {
        Map<String, List<String>> model = new HashMap<>();
        for (String key : theModel.keySet())
            if (!key.equals(queryType))
                model.put(key, theModel.get(key));


        AdhocQueryType adhocQueryType = new AdhocQueryType();
        for (String paramName : model.keySet()) {
            List<String> values = new ArrayList<>();
            for (String rawValue : model.get(paramName)) {
                String value = rawValue;
                if (DocRefSQParamTranslator.codedTypes.contains(paramName))
                    value = "('" + rawValue + "')";
                else
                    value = "'" + rawValue + "'";
                if (DocRefSQParamTranslator.acceptsMultiple.contains(paramName))
                    value = "(" + value + ")";
                values.add(value);
            }

            SlotType1 aSlot = Slot.makeSlot(paramName, values);
            adhocQueryType.getSlot().add(aSlot);

        }

        adhocQueryType.setId(sqid);
        QueryExpressionType queryExpressionType = new QueryExpressionType();
        adhocQueryType.setQueryExpression(queryExpressionType);

        AdhocQueryRequest adhocQueryRequest = new AdhocQueryRequest();
        ResponseOptionType responseOptionType = new ResponseOptionType();
        responseOptionType.setReturnComposedObjects(true);
        responseOptionType.setReturnType(leafClass ? "LeafClass" : "ObjectRef");
        adhocQueryRequest.setResponseOption(responseOptionType);
        adhocQueryRequest.setAdhocQuery(adhocQueryType);

        AhqrSender sender = new AhqrSender();
        sender.send(adhocQueryRequest, toAddr, task);

        return sender;
    }
}
