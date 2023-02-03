export default {
    data() {
        return {
        }
    },
    methods: {
        hasExtension(report, ext) {
            if (!report) return false;
            if (!report.extension) return false;
            let cond = false;
            report.extension.forEach(ex => {
                if (ex.url === ext)
                    cond = true;
            })
            return cond;
        },
        hasModifierExtension(actionReport, ext) {
            // console.debug(ext)
            /*
            --
            {"action":[{"modifierExtension":[{"url":"urn:asbestos:test:action:mayHaveBugsWhichRequireManualReview"}],"operation":{"modifierExtension":[{"url":"urn:moduleId","valueString":"StructureDefinitionValidation"},{"url":"urn:moduleName","valueString":"StructureDefinitionValidation"}],"result":"pass","message":"'igMayHaveBugs_1'"}}]}
             */
            if (actionReport === undefined || actionReport === null) return false
            let result = false
            // console.log('x' + JSON.stringify(actionReport))
            actionReport.action.forEach(actionEl => {
                if ('modifierExtension' in actionEl) {
                    actionEl.modifierExtension.forEach(o => {
                        if (o.url !== undefined && o.url === ext) {
                            result = true
                        }
                    })
                }
            })
            return result
        },
        isReportConditional(report) {
            return this.hasExtension(report, 'urn:conditional')
        },
        doesReportHaveBugExtension(report) {
            return this.hasModifierExtension(report, 'urn:asbestos:test:action:mayHaveBugsWhichRequireManualReview')
        },
    },
    computed: {
        colorStatus() {
            return this.$store.getters.testStatus
        },
        colorful() {
            return this.$store.state.testRunner.colorMode
        },
        statusRight() {
            return this.$store.state.testRunner.statusRight
        },

        // these can be called either with an this.report set to an action
        // or to a test/setup/teardown
        isPass() {
            if (!this.report) return false
            if (this.report.operation) return this.report.operation.result === 'pass'
            if (this.report.assert) return this.report.assert.result === 'pass'
            if (!this.report.action) return false
            let pass = true
            this.report.action.forEach(action => {
                const part = action.operation ? action.operation : action.assert
                const conditional = this.isReportConditional(part)
                if (part && part.result !== 'pass' && !conditional) pass = false
            })
            return pass
        },
        isTrue() {
            if (!this.report) return false
            if (this.report.operation) return this.report.operation.result === 'pass'
            if (this.report.assert) return this.report.assert.result === 'pass'
            if (!this.report.action) return false
            let isTrue = false
            this.report.action.forEach(action => {
                const part = action.operation ? action.operation : action.assert
                const conditional = this.isReportConditional(part)
                if (part && part.result !== 'pass' && conditional) isTrue = false
            })
            return isTrue
        },
        isError() {
            if (!this.report) return false
            if (this.report.operation) return this.report.operation.result === 'error'
            if (this.report.assert) return this.report.assert.result === 'error'
            if (!this.report.action) return false
            let error = false
            this.report.action.forEach(action => {
                const part = action.operation ? action.operation : action.assert
                if (part && part.result === 'error') error = true
            })
            return error
        },
        isManualReviewRequired() {
            if (this.report=== undefined || this.report ===null || this.report.action === undefined || this.report.action === null) return false
            let result = false
            result = this.doesReportHaveBugExtension(this.report)
            return result
        },
        isWarningOperation() {

            if (this.report=== undefined || this.report=== null) return false
            let result = false
            // console.log('x' + JSON.stringify(actionReport))
            this.report.action.forEach(actionEl => {
                if ('operation' in actionEl) {
                    result = actionEl.operation.result === 'warning'
                }
            })
            return result

        },
        isFail() {
            if (!this.report) return false
            if (this.report.operation) return this.report.operation.result === 'fail'
            if (this.report.assert) return this.report.assert.result === 'fail'
            if (!this.report.action) return false
            let fail = false
            this.report.action.forEach(action => {
                const part = action.operation ? action.operation : action.assert
                const conditional = this.isReportConditional(part)
                if (part && part.result === 'fail' && !conditional) fail = true
            })
            return fail
        },
        isFalse() {
            if (!this.report) return false
            if (this.report.operation) return this.report.operation.result === 'fail'
            if (this.report.assert) return this.report.assert.result === 'fail'
            if (!this.report.action) return false
            let fail = false
            this.report.action.forEach(action => {
                const part = action.operation ? action.operation : action.assert
                const conditional = this.isReportConditional(part)
                if (part && (part.result === 'fail' || part.result === 'error') && conditional) fail = true
            })
            return fail
        },
        isNotRun() {
            return !this.report
            // if (!this.report || !this.report.action) return true
            // let notRun = false
            // this.report.action.forEach(action => {
            //     const part = action.operation ? action.operation : action.assert
            //     if (part && part.result === 'skip') notRun = true
            // })
            // return notRun
        },
        isConditionFailed() {
            if (this.report && this.report.action) {
                let failed = false
                this.report.action.forEach(action => {
                    if (action.assert) {
                        if (action.assert.result === 'fail') failed = true
                    }
                })
                return failed
            }
            if (this.report && this.report.assert) {
                if (this.report.assert.result === 'fail') return true;
            }
            return false;
        },
    },
}
