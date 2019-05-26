package gov.nist.asbestos.toolkitApi.configDatatypes.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public enum FhirVerb implements Serializable, IsSerializable {
    READ, QUERY, TRANSACTION, NONE;
}
