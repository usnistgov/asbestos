<template>
    <div class="left">
        <h2>Setup</h2>
        <p>These are steps that must be completed when NIST FHIR Toolkit is installed. They can
        be re-run later to verify operation.</p>

        <h3>Load static Patient resources</h3>
        <p>These Patient resources are referenced in tests and must be loaded for tests to operate.</p>

    </div>
</template>

<script>
    export default {
        methods: {
            async loadPatients() {
                this.$store.commit('setTestCollectionName', 'Test_Patients')
                await this.$store.dispatch('loadCurrentTestCollection')
                const requiredChannel = this.$store.state.testRunner.requiredChannel
                if (requiredChannel) {
                    this.$store.commit('setChannelId', requiredChannel)
                }
                this.$store.dispatch('loadChannel', this.fullChannelId)
                    .then(channel => {
                        this.channelObj = channel
                    })
                const promises = []
                promises.push(this.$store.dispatch('loadTestScripts', this.$store.state.testRunner.testScriptNames))
                if (!this.$store.state.testRunner.isClientTest)
                    promises.push(this.$store.dispatch('loadTestReports', this.$store.state.testRunner.currentTestCollectionName))
                await Promise.all(promises)
            },
        },
        computed: {
            isPatientsLoaded() {
                return false;
            },
            isPatientsSuccess() {
                return false;
            },
        },
        created() {
            this.loadPatients()
        },
        name: "Setup"
    }
</script>

<style scoped>

</style>
