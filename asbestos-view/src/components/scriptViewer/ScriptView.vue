<template>
    <div class="instruction">
        <div class="big-bold">Script</div>
        <div>{{testCollection}}/{{testId}}</div>
    </div>
</template>

<script>
    export default {
        computed: {
            script() {
                this.loadTestScript();
                return this.$store.state.testRunner.testScripts[this.testId]
            },
            report() {
                this.loadTestReport();
                return this.$store.state.testRunner.testReports[this.testId]
            },
        },
        methods: {
            async loadTestScript() {
                if (this.$store.state.testRunner.testScripts[this.testId] === null)
                    await this.$store.dispatch('loadTestScript', {testCollection: this.testCollection, testId: this.testId});
            },
            async loadTestReport() {
                if (this.$store.state.testRunner.testReports[this.testId] === null)
                    await this.$store.dispatch('loadTestReport', {testCollection: this.testCollection, testId: this.testId});
            },
        },
        props: [
            'sessionId', 'channelId', 'testCollection', 'testId'
        ],
        name: "ScriptView"
    }
</script>

<style scoped>

</style>
