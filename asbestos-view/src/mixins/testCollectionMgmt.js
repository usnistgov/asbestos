import {UtilFunctions} from "../common/http-common";

export default {
    data() {
        return {
            tcLoading: false,
            // channelObj: this.theChannelObj,  // channel object
            testOpen: false,
            evalCount: 30,
            eventsForMinimalClientCollection: 56, /*  According to the EC channel directory, 56 events are generated for v3 Limited TC */
            eventsForComprehensiveClientCollection: 93, /* According to the EC channel directory, 93 events generated for v3 Comprehensive TC */
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
            //         // console.debug('In testScriptNamesUpdated: ' + name)
            //         // console.debug(this.$store.state.log.eventSummaries === undefined || this.$store.state.log.eventSummaries === null)
            //         // console.debug(this.$store.state.log.eventSummaries.length)
            //         // console.debug(this.$store.state.log.loaded )
            // This auto-run of client tests causes a potential for run-away tests if the test collection was switched to another one, causing the internal testScript Vue store to be corrupted.
            // The result is a run test request is made up of invalid combination of testCollection + testId such as: documentRecipientMin (server test collection) + Single_Document (client test)
            // Example: https://fhirtoolkit.test:9743/asbestos/engine/testScript/MHD_DocumentRecipient_minimal/Single_Document?crossdomain=true
            // To overcome the run-away tests, test collection list boxes and other items will be disabled while tests are running.
            if (this.isClient) {
                this.doRunAll()
            }
        },
        // run testName of testCollection
        async doRun(testName, testRoutePath) {  // server tests
            if (!testName)
                return
            this.$store.commit('setRunning',true)
            this.beginTestTime()
            this.$store.commit('setCurrentTest', null)
            await this.$store.dispatch('runTest', testName)
            this.$store.commit('setCurrentTest', testName)
            this.$store.commit('setRunning',false)
            this.endTestTime()
            const currentRoutePath = this.$router.currentRoute.path
            const testRoutePathToBe = `${testRoutePath}/${testName}`
            if (currentRoutePath !== testRoutePathToBe) {
                this.$router.push(testRoutePathToBe)
            }
        },
        async doEval(testName) {  // run single client test
            // console.debug('In doEval')
            if (testName) {
                this.$store.commit('setRunning',true)
                this.beginTestTime()
                await this.$store.dispatch('runEval', testName)
                this.$store.commit('setRunning',false)
                this.endTestTime()
            }
        },
        // run all tests in collection
        async doRunAll()  {
            // console.debug('In doRunAll')
            this.$store.commit('setRunning',true)
            this.beginTestTime()
            if (! this.isClient) {
                this.doClearLogs(true)
            }
            for (const name of this.scriptNames) {
                if (this.isClient) { // collection is client or server
                    await this.$store.dispatch('runEval', name)
                } else {
                    await this.$store.dispatch('runTest', name)
                }
            }
            this.$store.commit('setRunning',false)
            this.endTestTime()
        },
        async doClearLogs(silent=false) {
            if (silent || confirm('Temporarily clear all test reports for this Test Collection?')) {
                await this.$store.commit('clearTestReports')
            }
        },
        // async loadAChannel(channelId) {
            // let promise =
            // await this.$store.dispatch('loadChannel', channelId)
            // this.channelObj = await promise;
        // },
        async loadTestCollection(testCollection) {
            // console.debug('In loadTestCollection')
            this.tcLoading = true
            // this.$store.dispatch('loadChannel', this.fullChannelId).then(() => {
            if (this.$store.state.testRunner.currentTestCollectionName !== testCollection) {
                this.$store.commit('setTestCollectionName', testCollection)
            }
                this.$store.dispatch('loadCurrentTestCollection').then(() =>{
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
                    // promises.push(new Promise ((resolve  ) => {
                    //     console.log('Done loading scripts and reports')
                    //     resolve(true)
                    // }))
                    // promises.push(new Promise(() =>
                    // {
                    //     this.tcLoading = false
                    // }))
                Promise.all(promises).then(() => {
                        this.tcLoading = false
                        this.testScriptNamesUpdated()
                        console.log('Done loading scripts and reports')
                })
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
            // console.debug('In beginTestTime')
            try {
                if (this.$store.getters.testTimer !== null) {
                    // clearInterval(this.$store.getters.testTimer)
                    this.$store.commit('clearTestTimerInterval')
                }
                this.$store.commit('setTestTimerBeginTime', new Date())
                this.calcTestTime()
                if (this.$store.getters.testTimer === null) {
                    this.$store.commit('setTestTimer', setInterval(this.calcTestTime, 100))
                } else {
                    console.error('testTimer is already active, beginTestTime is cancelled.')
                }
            } catch (e) {
                console.error('beginTestTime error ' + e)
            }
        },
        calcTestTime() {
            try {
                const currentTcName = this.$store.state.testRunner.currentTestCollectionName
                if (currentTcName === undefined || currentTcName === null || currentTcName === '') {
                    console.error('calcTestTime: currentTcName is invalid')
                } else {
                    if (this.$store.getters.tcTestTimerElapsed[currentTcName] === undefined || this.$store.getters.tcTestTimerElapsed[currentTcName] === null) {
                        this.$store.commit('setTestTimerElapsed', {tcName: currentTcName, milliSeconds: 0})
                    }
                    const diffTm = new Date().getTime() - this.$store.getters.testTimerBeginTime.getTime()
                    this.$store.commit('setTestTimerElapsed', {tcName: currentTcName, milliSeconds: diffTm})
                    // console.debug(currentTcName + ' isNaN: ' + isNaN(this.$store.getters.tcTestTimerElapsed(currentTcName)))
                }
            } catch(e) {
                console.error('calcTestTime exception: ' + e)
            }
        },
        endTestTime() {
            if (this.testTimer !== null) {
                // clearInterval(this.$store.getters.testTimer)
                this.$store.commit('clearTestTimerInterval')
                this.$store.commit('setTestTimer', null)
                this.calcTestTime()
            }
        },
    },
    computed: {
        running() {
            return this.$store.getters.isRunning
        },
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
            return `${UtilFunctions.getProxyBase()}/${this.sessionId}__${this.channelName}`
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
        tlsOption: {
            set(use) {
                this.$store.commit('setUseTlsProxy', use)
            },
            get() {
                return this.$store.state.testRunner.useTlsProxy
            }
        },
        fullChannelId() {
            return `${this.sessionId}__${this.channelName}`
        },
        elapsedTestTime() {
            try {
            // const currentTcName = this.$store.state.testRunner.currentTestCollectionName
                const currentTcName = this.$store.getters.currentTcName
                const s = this.$store.getters.tcTestTimerElapsed[currentTcName] / 1000
                // console.debug( 'isNan ..' + isNaN(s) + '. got ... ' + s)
                return Number(s).toFixed(1);
            //     return Number(this.tcTestTimerElapsedMilliSeconds[this.$store.getters.currentTcName] / 1000).toFixed(1)
            } catch {
                console.error('elapsedTestTime error')
               return 0
            }
        },
    }
}
