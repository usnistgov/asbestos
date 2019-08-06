package gov.nist.asbestos.mhd.translation.search;

import gov.nist.asbestos.mhd.transactionSupport.AhqrSender;
import gov.nist.asbestos.mhd.translation.attribute.Slot;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.ResponseOptionType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.AdhocQueryType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.QueryExpressionType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;

import java.net.URI;
import java.util.*;

import static gov.nist.asbestos.mhd.translation.search.DocRefSQParamTranslator.queryType;

public class FhirSq {

    /**
     *
     * @param query is param1=value1;param2=value2...
     * @return StoredQuery model
     */
    private static Map<String, List<String>> docRefQueryToSQModel(String query) {
        List<String> params = Arrays.asList(query.split(";"));
        return new DocRefSQParamTranslator().run(params);
    }

    public static AhqrSender docRefQuery(String httpQueryString, URI toAddr) {
        return run(docRefQueryToSQModel(httpQueryString), "urn:uuid:14d4debf-8f97-4251-9a74-a90016b0af0d" /* FindDocuments */, toAddr, true);
    }

    public static AhqrSender docRefQuery(List<String> queryParams, URI toAddr) {
        return run(new DocRefSQParamTranslator().run(queryParams), "urn:uuid:14d4debf-8f97-4251-9a74-a90016b0af0d" /* FindDocuments */, toAddr, true);
    }

    // model is [queryParamName: [values]]
    public static AhqrSender run(Map<String, List<String>> theModel, String sqid, URI toAddr, boolean leafClass) {
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
        sender.send(adhocQueryRequest, toAddr);

        return sender;
    }
}
