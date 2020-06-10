<template>
    <div class="left">
        <h2>MHD Testing</h2>
        Instructions for testing key MHD actors.

        <h3>MHD Document Source - Minimal</h3>

        <h4>Instructions</h4>
        <ol>
            <li>Choose Channel - limited.  Configuration shows FHIRBASE to send to.</li>
            <li>Select Test Collection. Send messages to satisfy test requirements listed.</li>
            <li>Run test to run validations.  It will search recent messages for requirements match.</li>
            <li>Alter number of recent events to evaluate as necessary.</li>
        </ol>

        <h4>Operation</h4>
        Document Source (SUT) sends Provide Document Bundle transactions to FHIRBase specified in the Limited Channel.
        Validation happens in three parts:

        <ol>
            <li>Upon receipt at the Limited Channel, the Bundle structure is evaluated and errors are returned in the response message.</li>
            <li>Metadata is translated into XDS format and forwarded to XDS Toolkit for evaluation (Repository/Registry simulator
                configured for Metadata Limited validation). Outcome is relayed in
            the response message.</li>
            <li>Additional evaluations are run from the Toolkit UI.  These are reported on screen and are not included in
            the response message.</li>
        </ol>


<!--        <div class="selectable" @click="configureMinimalDocumentSource()">Configure the tool and go to the tests</div>-->


        <h3>MHD Document Source - Comprehensive (Option)</h3>

        <h4>Instructions</h4>
        <ol>
            <li>Choose Channel - xds.  Configuration shows FHIRBASE to send to.</li>
            <li>Select Test Collection. Send messages to satisfy test requirements listed.</li>
            <li>Run test to run validations.  It will search recent messages for requirements match.</li>
            <li>Alter number of recent events to evaluate as necessary.</li>
        </ol>

        <h4>Operation</h4>
        Document Source (SUT) sends Provide Document Bundle transactions to FHIRBase specified in the xds Channel.
        Validation happens in three parts:

        <ol>
            <li>Upon receipt at the Limited Channel, the Bundle structure is evaluated and errors are returned in the response message.</li>
            <li>Metadata is translated into XDS format and forwarded to XDS Toolkit for evaluation (Repository/Registry simulator
                configured for Metadata Limited validation). Outcome is relayed in
                the response message.</li>
            <li>Additional evaluations are run from the Toolkit UI.  These are reported on screen and are not included in
                the response message.</li>
        </ol>

<!--        <div class="selectable" @click="configureComprehensiveDocumentSource()">Configure the tool and go to the tests</div>-->

        <h3>MHD Document Recipient - Minimal</h3>

        <h4>Instructions</h4>
        <ol>
            <li>Configure channel for your server.  sut is available for that purpose or create your own.</li>
            <li>Choose Channel.</li>
            <li>Select Test Collection.</li>
            <li>Run tests individually or as a group (runall).</li>
        </ol>

        FHIR Toolkit sends Provide Document Bundle transactions to the FHIRBase specified in the selected Channel.
        The response is validated.

<!--        <div class="selectable" @click="configureMinimalDocumentRecipient()">Configure the tool and go to the tests</div>-->


        <h3>MHD Document Recipient - Comprehensive (Option)</h3>

        <h4>Instructions</h4>
        <ol>
            <li>Configure channel for your server.  sut is available for that purpose or create your own.</li>
            <li>Choose Channel.</li>
            <li>Select Test Collection.</li>
            <li>Run tests individually or as a group (runall).</li>
        </ol>

        FHIR Toolkit sends Provide Document Bundle transactions to the FHIRBase specified in the SUT Channel.
        The response is validated.

<!--        <div class="selectable" @click="configureComprehensiveDocumentRecipient()">Configure the tool and go to the tests</div>-->


        <h3>MHD Document Consumer</h3>

        Tests under development

        <h3>MHD Document Responder</h3>
        Tests under development

        <h2>Inspector</h2>
        The Inspector is a tool for viewing the Proxy logs in raw mode (as they were sent on the wire) or graphical
        form.  The contents of a Provide Document Bundle can be displayed as well as the related content extracted
        from the server.

        The Inspector can be entered:

        <ul>
            <li>From the running of a Test Collection (open a test, open a transaction/action and
                select *Message Log*).</li>
            <li>From the Getter (automatically opened at completion of HTTP GET.</li>
            <li>From the Event Viewer.  Select an event and the Inspector opens.</li>
        </ul>
    </div>
</template>

<script>
    export default {
        methods: {
            configureMinimalDocumentSource() {
                const collection = 'MHD_DocumentSource_minimal'
                const channelId = 'limited'
                this.go(collection, channelId)
            },
            configureComprehensiveDocumentSource() {
                const collection = 'MHD_DocumentSource_comprehensive'
                const channelId = 'xds'
                this.go(collection, channelId)
            },
            configureMinimalDocumentRecipient() {
                const collection = 'MHD_DocumentRecipient_minimal'
                const channelId = 'sut'
                this.go(collection, channelId)
            },
            configureComprehensiveDocumentRecipient() {
                const collection = 'MHD_DocumentRecipient_comprehensive'
                const channelId = 'sut'
                this.go(collection, channelId)
            },
            go(collection, channelId) {
                this.$store.commit('setChannelId', channelId)
                this.$store.commit('setTestCollectionName', collection)
                this.$store.commit('setTestCollectionName', collection)
                this.$router.push(`/session/${this.$store.state.base.session}/channel/${this.$store.state.base.channelId}/collection/${collection}`)
            },
        },
        name: "MhdTesting"
    }
</script>

<style scoped>

</style>
