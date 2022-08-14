NOTICE:
Since the FHIRToolkit proxy only has a single Proxy\FHIR-base, it was not possible to distinguish the MHD actor in question (MHD Consumer or MHD Recipient or MHD Responder). Hence, the capability statements of the MHD Document Responder and the MHD Document Recipient can be combined into one capability statement.

These resource elements below are dynamically added based on the channel mhdVersion property:

    <software>
        <name value="FHIRToolkit"/>
        <version value="${fhirToolkitVersion}"/>
    </software>
    <implementation>
        <description value="MHD DocumentRecipient"/>
        <url value="${fhirToolkitBase}/proxy/${channelId}"/>
    </implementation>

 <rest>
        <mode value="server"/>
        <interaction>
            <code value="transaction"/>
            <documentation value="http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Minimal_DocumentBundle"/>
        </interaction>
    </rest>