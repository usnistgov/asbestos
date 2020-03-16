export default {
    computed: {
        isPass() {
            if (!this.report) return false
            let pass = true
            this.report.action.forEach(action => {
                const part = action.operation ? action.operation : action.assert
                if (part && part.result !== 'pass') pass = false
            })
            return pass
        },
        isError() {
            if (!this.report) return false
            let error = false
            this.report.action.forEach(action => {
                const part = action.operation ? action.operation : action.assert
                if (part && part.result === 'error') error = true
            })
            return error
        },
        isFail() {
            if (!this.report) return false
            let fail = false
            this.report.action.forEach(action => {
                const part = action.operation ? action.operation : action.assert
                if (part && part.result === 'fail') fail = true
            })
            return fail
        },
        isNotRun() {
            if (!this.report) return true
            let notRun = false
            this.report.action.forEach(action => {
                const part = action.operation ? action.operation : action.assert
                if (part && part.result === 'skip') notRun = true
            })
            return notRun
        },
        isConditionFailed() {
            if (!this.report) return false
            let failed = false
            this.report.action.forEach(action => {
                if (action.assert) {
                    if (action.assert.result === 'fail') failed = true
                }
            })
            return failed
        },
    },
}
