<template>
    <div class="left">
        <h2>Client testing configuration</h2>

        <p>In this mode, FHIR Toolkit accepts messages from a client-style actor such as the MHD Document Source and
            and validates them.</p>

        <img src="../../assets/Client_workflow.png">
        <h3>Processing</h3>
        <ol>
            <li>Tool is put in client testing mode by selecting a Client test collection.</li>
            <li>SUT sends one or more transactions to satisfy demands of on-screen documentation. The FHIRBASE
                to send to is displayed on-screen. This FHIRBASE leads to the Proxy.</li>
            <li>Messages received by the proxy are recorded for later viewing and forwarded on to a pre-configured
                service.</li>
            <li>Messages passing through the proxy use one of several Channels. The channel configuration controls
                where the message is sent by the proxy and how it is transformed before sending.  For client tests...</li>
            <li>Testing of MHD minimal metadata (by selecting a test collection labeled "minimal"), causes the outbound
                messages to be sent to a XDS Toolkit Repository/Registry simulator configured to accept limited metadata.</li>
            <li>There are two proxy channel types: FHIR and MHD. An MHD type channel is used for client testing. The
                proxy expects to receive Provide Document Bundle transactons. It translates them to XDS Provide and Register
                transactions before forwarding.</li>
        </ol>

        <h3>Validation</h3>

        <span class="bold">In the Proxy: </span>
        <ul>
            <li>Validates the Provide Document Bundle transaction bundle structure.</li>
            <li>Validates proper FHIR coding of attributes.</li>
            <li>Does not validate the presence of specific metadata attributes.</li>
        </ul>

        <span class="bold">In the XDS Toolkit simulator</span>
        <ul>
            <li>All aspects of the Provide and Register  are validated including presence of required attributes, object
                linking, coding, etc.</li>
            <li>Metadata and documents are saved (this is not important for validation, just a side-effect.</li>
            <li></li>
        </ul>

        The results of these two phases of validation are returned to the client SUT.  Final validation is done from the
        FHIR Toolkit user interface. This process validates that the transaction returned no errors and that top level
        requirements were met.  An example  of top level requirements are that the transaction submitted two documents.

        <h3>Configuring FHIR Toolkit for this testing</h3>

        <p>When installed, FHIR Toolkit has two pre-configured channels named limited and xds in the proxy.  These channels
            are used for handling Comprehensive (xds channel) or Minimal (limited channel) metadata. The channel configurations
            for these two channels cause messages to be forwarded to two different simulators in XDS Toolkit with different
            sets of validations enabled.

        <p>It is critical that Test Collection and Channel are selected appropriately to meet these needs. The MHD Testing
            window (see top menu banner) has links that configure FHIR Toolkit for a particular testing challenge.

        <h2>Server testing configuration</h2>

        <p>In this mode, FHIR Toolkit initiates messages to a server-style actor such as the MHD Document Recipient and validates
        the response.</p>
        <img src="../../assets/Server_workflow.png">

        <h3>Processing</h3>

        <ol>
            <li>Server testing uses the SUT channel in the Proxy. Before a test can be run the SUT channel
            configuration must be edited and the correct FHIRBase for the SUT inserted.</li>
            <li>Tool is put in server testing mode by selecting a Server test collection.</li>
            <li>The list applicable tests are displayed and individually run or run as a group.</li>
            <li>Initiating a test from the user interface instructs the Test Engine to contact the SUT,
            sending messages through the Proxy. In this mode a FHIR Channel in the Proxy is used so the messages
            pass through unchanged except for addressing-related headers.</li>
            <li>The response from the SUT returns through the Proxy.</li>
            <li>The response is graded by the Test Engine which can evaluate the response and also issue
            secondary requests such as queries to validate the content of the server.</li>
        </ol>

        <h3>Validation</h3>
        <p>All validation is done by evaluating the response message or by sending secondary requests such as
        queries to validate the content of the server.</p>

        <h3>Configuring FHIR Toolkit for this testing</h3>
        <p>Server testing uses the SUT channel in the Proxy. Before a test can be run the SUT channel
            configuration must be edited and the correct FHIRBase for the SUT inserted.</p>
    </div>
</template>

<script>
    export default {
        name: "Configurations"
    }
</script>

<style scoped>

</style>
