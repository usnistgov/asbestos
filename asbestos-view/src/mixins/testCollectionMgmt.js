export default {
    data() {
        return {
            running: false,
            channelObj: null,  // channel object
            testOpen: false,
            evalCount: 5,
        }
    },
    methods: {
        setEvalCount() {
            this.$store.commit('setEventEvalCount', this.evalCount)
        },
        async testScriptNamesUpdated() {
            if (this.isClient) {
                return this.$store.state.testRunner.testScriptNames.forEach(name => {
                    this.doEval(name)
                })
            }
        },
        async doRunAll()  {
            this.running = true
            for (const name of this.scriptNames) {
                if (this.isClient)
                    await this.doEval(name)
                else
                    await this.$store.dispatch('runTest', name)
            }
            this.running = false
        },
        async loadTestCollection(testCollection) {
            this.$store.commit('setTestCollectionName', testCollection)
            await this.$store.dispatch('loadCurrentTestCollection')
            this.testScriptNamesUpdated()
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
        cleanTestName(text) {
            if (text)
                return text.replace(/_/g, ' ')
            return ''
        },
        async doEval(testName) {  // client tests
            if (testName)
                await this.$store.dispatch('runEval', testName)
        },
        async doRun(testName) {  // server tests
            if (!testName)
                return
            this.running = true
            await this.$store.dispatch('runTest', testName)
            this.running = false
        },
        async doDebug(testName) {  // server tests
            if (!testName)
                return
            this.running = true
            await this.$store.dispatch('runTest', testName)
            this.running = false
        },
        doJson() {
            this.$store.commit('setUseJson', true)
        },
        doXml() {
            this.$store.commit('setUseJson', false)
        },
    },
    computed: {
        clientBaseAddress() { // for client tests
            return `${this.$store.state.base.proxyBase}/${this.sessionId}__${this.channelId}`
        },
        json: {
            get() {
                return this.$store.state.testRunner.useJson
            }
        },
        gzip: {
            set(use) {
                this.$store.commit('setUseGzip', use)
            },
            get() {
                return this.$store.state.testRunner.useGzip
            }
        },
        channel: {
            set(name) {
                if (name !== this.$store.state.base.channelId) {
                    this.$store.commit('setChannelId', name)
                }
            },
            get() {
                return this.$store.state.base.channelId
            }
        },
        fullChannelId() {
            return `${this.sessionId}__${this.channelId}`
        },
    }
}
