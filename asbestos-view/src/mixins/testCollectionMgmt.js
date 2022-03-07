import Vue from 'vue'
import {UtilFunctions} from "../common/http-common";

export default {
    data() {
        return {
            tcLoading: false,
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
            //         // console.debug('In testScriptNamesUpdated: ' + name)
            //         // console.debug(this.$store.state.log.eventSummaries === undefined || this.$store.state.log.eventSummaries === null)
            //         // console.debug(this.$store.state.log.eventSummaries.length)
            //         // console.debug(this.$store.state.log.loaded )
            // This auto-run of client tests causes a potential for run-away tests if the test collection was switched to another one, causing the internal testScript Vue store to be corrupted.
            // The result is a run test request is made up of invalid combination of testCollection + testId such as: documentRecipientMin (server test collection) + Single_Document (client test)
            // Example: https://fhirtoolkit.test:9743/asbestos/engine/testScript/MHD_DocumentRecipient_minimal/Single_Document?crossdomain=true
            // if (this.isClient) {
            //     this.doRunAll()
            // }
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
            clearInterval(this.testTimer)
            this.testTimerBeginTime = new Date()
            this.calcTestTime()
            if (this.testTimer === null) {
                this.testTimer = setInterval(this.calcTestTime, 100);
            } else {
                console.error('testTimer is already active, beginTestTime is cancelled.')
            }
        },
        calcTestTime() {
            try {
                const currentTcName = this.$store.state.testRunner.currentTestCollectionName
                if (this.tcTestTimerElapsedMilliSeconds[currentTcName] === undefined || this.tcTestTimerElapsedMilliSeconds[currentTcName] === null) {
                    Vue.set(this.tcTestTimerElapsedMilliSeconds, currentTcName, 0)
                }
                Vue.set(this.tcTestTimerElapsedMilliSeconds, currentTcName, new Date().getTime() - this.testTimerBeginTime.getTime())
            } catch(e) {
                console.error('calcTestTime exception: ' + e)
            }
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
                const currentTcName = this.$store.state.testRunner.currentTestCollectionName
                const s = this.tcTestTimerElapsedMilliSeconds[currentTcName] / 1000
                return Number(s).toFixed(1);
            } catch {
               return 0
            }
        },
    }
}
