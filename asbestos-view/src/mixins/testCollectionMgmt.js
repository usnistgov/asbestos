import Vue from 'vue'

export default {
    data() {
        return {
            running: false,
            // channelObj: this.theChannelObj,  // channel object
            testOpen: false,
            evalCount: 30,
            testTimerBeginTime : new Date(),
            testTimer: null,
            tcTestTimerElapsedMilliSeconds: {},
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
        async doRun(testName, testRoutePath) {  // server tests
            if (!testName)
                return
            this.running = true
            this.beginTestTime()
            this.$store.commit('setCurrentTest', null)
            await this.$store.dispatch('runTest', testName)
            this.$store.commit('setCurrentTest', testName)
            this.running = false
            this.endTestTime()
            const currentRoutePath = this.$router.currentRoute.path
            const testRoutePathToBe = `${testRoutePath}/${testName}`
            if (currentRoutePath !== testRoutePathToBe) {
                this.$router.push(testRoutePathToBe)
            }
        },
        async doEval(testName) {  // client tests
            if (testName)
                await this.$store.dispatch('runEval', testName)
        },
        // run all tests in collection
        async doRunAll()  {
            this.running = true
            this.beginTestTime()
            this.doClearLogs(true)
            for (const name of this.scriptNames) {
                if (this.isClient) { // collection is client or server
                    await this.$store.dispatch('runEval', name)
                } else {
                    await this.$store.dispatch('runTest', name)
                }
            }
            this.running = false
            this.endTestTime()
        },
        async doClearLogs(silent=false) {
            if (silent || confirm('Clear all test reports for this Test Collection?')) {
                await this.$store.commit('clearTestReports')
            }
        },
        // async loadAChannel(channelId) {
            // let promise =
            // await this.$store.dispatch('loadChannel', channelId)
            // this.channelObj = await promise;
        // },
        async loadTestCollection(testCollection) {
            // this.$store.dispatch('loadChannel', this.fullChannelId).then(() => {
                this.$store.commit('setTestCollectionName', testCollection)
                this.$store.dispatch('loadCurrentTestCollection').then(() =>{
                    this.testScriptNamesUpdated()
                    const requiredChannel = this.$store.state.testRunner.requiredChannel
                    if (requiredChannel) {
                        console.log(`required channel is ${requiredChannel}`)
                        this.$store.commit('setChannelId', requiredChannel)
                    }
                    // await this.loadAChannel(this.fullChannelId);
                    //  this.channelObj = p
                    const promises = []
                    promises.push(this.$store.dispatch('loadTestScripts', this.$store.state.testRunner.testScriptNames))
                    if (!this.$store.state.testRunner.isClientTest) {
                        promises.push(this.$store.dispatch('loadTestReports', this.$store.state.testRunner.currentTestCollectionName))
                        promises.push(this.$store.dispatch('loadNonCurrentTcTestReports')) // for test dependency purposes
                    }
                    promises.push(new Promise ((resolve  ) => {
                        console.log('Done loading scripts and reports')
                        resolve(true)
                    }))
                Promise.all(promises)
            })
            // })
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
        beginTestTime() {
            this.testTimerBeginTime = new Date()
            this.calcTestTime()
            if (this.testTimer === null) {
                this.testTimer = setInterval(this.calcTestTime, 100);
            } else {
                console.error('testTimer is already active, beginTestTime is cancelled.')
            }
        },
        calcTestTime() {
            const currentTcName = this.$store.state.testRunner.currentTestCollectionName
            if (this.tcTestTimerElapsedMilliSeconds[currentTcName] === undefined) {
                Vue.set(this.tcTestTimerElapsedMilliSeconds,currentTcName,  0)
            }
            Vue.set(this.tcTestTimerElapsedMilliSeconds, currentTcName,  new Date().getTime() - this.testTimerBeginTime.getTime())
        },
        endTestTime() {
            if (this.testTimer !== null) {
                clearInterval(this.testTimer)
                this.testTimer = null
                this.calcTestTime()
            }
        },
    },
    computed: {
        theChannelObj() {
         return this.$store.state.base.channel
        },
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
            return `${this.$store.state.base.proxyBase}/${this.sessionId}__${this.channelName}`
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
        fullChannelId() {
            return `${this.sessionId}__${this.channelName}`
        },
        elapsedTestTime() {
            try {
                const currentTcName = this.$store.state.testRunner.currentTestCollectionName
                const s = this.tcTestTimerElapsedMilliSeconds[currentTcName] / 1000
                return Number(s).toFixed(1);
            } catch {
               return 0
            }
        },
    }
}
