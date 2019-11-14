<template>
    <div class="left">
        <h2>MHD Testing</h2>

        <h3>MHD Document Source</h3>

        <h4>Minimal</h4>
        Document Source (SUT) sends Provide Document Bundle transactions to FHIRBase specified in the Limited Channel.
        Validation happens in three parts:

        <ol>
            <li>Upon receipt at the Limited Channel, the Bundle is evaluated and errors are returned in the response message.</li>
            <li>Metadata is translated into XDS format and forwarded to XDS Toolkit for evaluation. Outcome is relayed in
            the response message.</li>
            <li>Test specific evaluations are run from the Toolkit UI.  These are reported on screen and are not included in
            the response message.</li>
        </ol>

        To run:
        <ol>
            <li><div class="selectable" @click="configureMinimalDocumentSource()">Configure the tool and go to the tests</div></li>
        </ol>

        <h4>Comprehensive</h4>

        <h3>MHD Document Recipient</h3>

        <h3>MHD Document Consumer</h3>

        <h3>MHD Document Responder</h3>
    </div>
</template>

<script>
    export default {
        methods: {
            configureMinimalDocumentSource() {
                const collection = 'MHD_DocumentSource'
                const channelId = 'limited'
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
