export default {
    data() {
        return {
        }
    },
     methods: {
        getTestScriptIndexKey(testScriptIndex) {
            const testCollectionIndex = this.$store.state.testRunner.serverTestCollectionNames.indexOf(this.testCollection)
            const key = testCollectionIndex + '.' + testScriptIndex // Follow proper key format
            return key
        },
        isDebuggable(testScriptIndex) {
            const key = this.getTestScriptIndexKey(testScriptIndex)
            return key in this.$store.state.debugTestScript.showDebugButton && Boolean(this.$store.state.debugTestScript.showDebugButton[key])
        },
        getDebugActionButtonLabel(testScriptIndex) {
            const key = this.getTestScriptIndexKey(testScriptIndex)
            if (key in this.$store.state.debugTestScript.showDebugButton) {
                let valObj = this.$store.state.debugTestScript.showDebugButton[key]
                if (valObj != undefined) {
                    return valObj.debugButtonLabel
                }
                // return "Debug"
                return "X"
            }
        },
         getBreakpointCount(testScriptIndex) {
             const key = this.getTestScriptIndexKey(testScriptIndex)
             if (key in this.$store.state.debugTestScript.showDebugButton) {
                 const breakpointSet = this.$store.state.debugTestScript.breakpointMap.get(key)
                 if (breakpointSet)
                    return breakpointSet.size
             }
             return 0
         },
         isEvaluable(testScriptIndex) {
            return (this.getDebugActionButtonLabel(testScriptIndex) === 'Resume') && this.$store.state.debugTestScript.evalMode
        },
        isDebugKillable(testScriptIndex) {
            return (this.getDebugActionButtonLabel(testScriptIndex) === 'Resume')
        },
        async doDebugKill(testScriptIndex) {
            await this.$store.dispatch('debugKill', this.getTestScriptIndexKey(testScriptIndex))
        },
        async doDebug(testName) {  // server tests
            if (!testName)
                return
            await this.$store.dispatch('debugTestScript', testName)
        },
        async doDebugEvalMode() {
            await this.$store.dispatch('doDebugEvalMode')
        },
        getBreakpointIndex(testType, testIndex, actionIndex) {
            return testType + testIndex + (actionIndex !== undefined ?   "." + actionIndex : "")
        },
        getBreakpointObj(breakpointIndex) {
            let obj = {testScriptIndex: this.currentMapKey, breakpointIndex: breakpointIndex}
            return obj
        },
        toggleBreakpointIndex(obj) {
            // console.log("enter toggleBreakpointIndex")
            // let obj = {testScriptIndex: testScriptIndex, breakpointIndex: testType + testIndex + "." + actionIndex}
            if (! this.$store.getters.hasBreakpoint(obj)) {
                // this.hoverActionIndex = actionIndex // Restore the hoverActionIndex when toggle on the same item goes from on(#)-off(-1)-on(#)
                // console.log("calling dispatch" + testScriptIndex + " breakpointIndex: " + testIndex + "." + actionIndex)
                return this.$store.dispatch('addBreakpoint', obj)

            } else {
                // this.hoverActionIndex = -1 // Immediately remove the debug indicator while the mouse hover is still active but without having to wait for the mouseLeave event
                // remove breakpoint
                //  console.log("calling removeBreakpoint dispatch" + testScriptIndex + " breakpointIndex: " + testIndex + "." + actionIndex)
                return this.$store.dispatch('removeBreakpoint', obj)
            }
        },
        debugTitle(testScriptIndex, testType, testIndex, actionIndex) {
            let obj = {testScriptIndex: testScriptIndex, breakpointIndex: testType + testIndex + "." + actionIndex}
            return this.$store.getters.getDebugTitle(obj);
        },
        closeModal() {
            this.$store.commit('setShowDebugEvalModal', false)
        },
    },
    computed: {
        currentMapKey()  {
            const testId = this.$store.state.testRunner.currentTest
            const mapKey = this.$store.getters.getMapKey(testId)
            return mapKey
        },
    }
}