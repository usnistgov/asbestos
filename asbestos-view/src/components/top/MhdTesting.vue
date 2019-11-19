<template>
    <div class="left">
        <h2>MHD Testing</h2>
        Instructions for testing key MHD actors.

        <h3>MHD Document Source</h3>

        <h4>Minimal</h4>
        Document Source (SUT) sends Provide Document Bundle transactions to FHIRBase specified in the Limited Channel.
        Validation happens in three parts:

        <ol>
            <li>Upon receipt at the Limited Channel, the Bundle structure is evaluated and errors are returned in the response message.</li>
            <li>Metadata is translated into XDS format and forwarded to XDS Toolkit for evaluation. Outcome is relayed in
            the response message.</li>
            <li>Test specific evaluations are run from the Toolkit UI.  These are reported on screen and are not included in
            the response message.</li>
        </ol>

        <div class="selectable" @click="configureMinimalDocumentSource()">Configure the tool and go to the tests</div>


        <h4>Comprehensive (Option)</h4>
        Document Source (SUT) sends Provide Document Bundle transactions to FHIRBase specified in the XDS Channel.
        Validation happens in three parts:

        <ol>
            <li>Upon receipt at the XDS Channel, the Bundle is evaluated and errors are returned in the response message.</li>
            <li>Metadata is translated into XDS format and forwarded to XDS Toolkit for evaluation. Outcome is relayed in
                the response message.</li>
            <li>Test specific evaluations are run from the Toolkit UI.  These are reported on screen and are not included in
                the response message.</li>
        </ol>

        <div class="selectable" @click="configureComprehensiveDocumentSource()">Configure the tool and go to the tests</div>

        <h3>MHD Document Recipient</h3>
        FHIR Toolkit sends Provide Document Bundle transactions to the FHIRBase specified in the SUT Channel.
        The response is validated.


        <h4>Minimal</h4>

        <div class="selectable" @click="configureMinimalDocumentRecipient()">Configure the tool and go to the tests</div>


        <h4>Comprehensive (Option)</h4>

        <div class="selectable" @click="configureComprehensiveDocumentRecipient()">Configure the tool and go to the tests</div>


        <h3>MHD Document Consumer</h3>


        <h3>MHD Document Responder</h3>
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
