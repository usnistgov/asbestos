export default {
    data() {
        return {
            running: false,
            channelObj: null,  // channel object
            testOpen: false,
            evalCount: 30,
        }
    },
    methods: {
        testTime(name) {  // time of test run for test name in testCollection
            const report = this.$store.state.testRunner.testReports[name]
            if (!report)
                return null
            return report.issued
        },
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
        // run testName of testCollection
        async doRun(testName) {  // server tests
            if (!testName)
                return
            this.running = true
            this.$store.commit('setCurrentTest', null)
            await this.$store.dispatch('runTest', testName)
            this.$store.commit('setCurrentTest', testName)
            this.running = false
        },
        async doEval(testName) {  // client tests
            if (testName)
                await this.$store.dispatch('runEval', testName)
        },
        // run all tests in collection
        async doRunAll()  {
            this.running = true
            for (const name of this.scriptNames) {
                if (this.isClient)  // collection is client or server
                    await this.$store.dispatch('runEval', name)
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
        doJson() {
            this.$store.commit('setUseJson', true)
        },
        doXml() {
            this.$store.commit('setUseJson', false)
        },
    },
    computed: {
        collectionStatus() {  // 'not-run', 'pass', 'fail', 'error'
            const statuses = this.status;   // object of status indexed by testId
            let collectionStatus = 'pass'
            statuses.forEach(status => {
                switch (status) {
                    case 'not-run':
                        if (collectionStatus === 'pass') collectionStatus = 'not-run';
                        break;
                    case 'error':
                        collectionStatus = 'error';
                        break;
                    case 'fail':
                        if (collectionStatus !== 'error') collectionStatus = 'fail';
                        break;
                }
            })
            return collectionStatus;
        },
        collectionTime() {
            let collectionTime = null;
            this.scriptNames.forEach(testName => {
                const testTime = this.testTime(testName)
                if (collectionTime === null)
                    collectionTime = testTime;
                if (testTime < collectionTime)
                    collectionTime = testTime;
            })
            return collectionTime;
        },
        status() { // status object for collection indexed by testId
            return this.$store.getters.testStatus
        },
        isClient() {
            return this.$store.state.testRunner.isClientTest
        },
        scriptNames: {
            get() {
                return this.$store.state.testRunner.testScriptNames
            }
        },
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
