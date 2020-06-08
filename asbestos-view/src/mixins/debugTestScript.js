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
        isPreviousDebuggerStillAttached(testScriptIndex) {
            const key = this.getTestScriptIndexKey(testScriptIndex)
            const indexList = this.$store.state.debugTestScript.debugMgmtIndexList
            if (indexList !== null || indexList !== undefined) {
                // return  indexList.filter(o => o.testScriptIndex === key).length === 1
                return indexList.includes(key)
            }
            return false
        },
         async removeDebugger(testScriptIndex) {
             const key = this.getTestScriptIndexKey(testScriptIndex)
             await this.$store.dispatch('debugMgmt', {'cmd':'removeDebugger','testScriptIndex':key})
         },
        getDebugActionButtonLabel(testScriptIndex) {
            const key = this.getTestScriptIndexKey(testScriptIndex)
            if (key in this.$store.state.debugTestScript.showDebugButton) {
                let valObj = this.$store.state.debugTestScript.showDebugButton[key]
                if (valObj != undefined) {
                    return valObj.debugButtonLabel
                }
            }
            return "X"
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
         getBreakpointsInDetails(obj) {
             let retObj = {'key': obj.breakpointIndex, 'childCount' : 0}
             const breakpointMap = this.$store.state.debugTestScript.breakpointMap
             if (breakpointMap.has(obj.testScriptIndex)) {
                 const breakpointSet = breakpointMap.get(obj.testScriptIndex)

                 var searchFn = function mySearchFn(currentVal /*, currentKey, set */) {
                     if (currentVal) {
                         if (currentVal.startsWith(this.key) && currentVal.length > this.key.length) {
                             this.childCount++
                         }
                     }
                 }
                 breakpointSet.forEach(searchFn, retObj /* retObj becomes 'this' inside the searchFn*/)
             }
             return retObj.childCount
         },
         isEvaluable(testScriptIndex) {
            return (this.getDebugActionButtonLabel(testScriptIndex) === 'Resume') && this.$store.state.debugTestScript.evalMode
        },
        isDebugging(testScriptIndex) {
            return (this.getDebugActionButtonLabel(testScriptIndex) === 'Resume')
        },
        async stopDebugging(testScriptIndex) {
            await this.$store.dispatch('stopDebugTs', this.getTestScriptIndexKey(testScriptIndex))
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
            if (! this.$store.getters.hasBreakpoint(obj)) {
                return this.$store.dispatch('addBreakpoint', obj)

            } else {
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
        displayAdditionalIndexLabel(isDisplayOpen, breakpointIndex) {
            let bkptOptionEl = document.querySelector("span.breakpointGutterOption[data-breakpoint-index='"+ breakpointIndex + "']")
            if (bkptOptionEl) {
                if (isDisplayOpen) {
                    bkptOptionEl.classList.add('breakpointOptionHidden')
                } else {
                    bkptOptionEl.classList.remove('breakpointOptionHidden')
                }
            }
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