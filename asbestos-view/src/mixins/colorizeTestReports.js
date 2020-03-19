export default {
    data() {
        return {
        }
    },
    methods: {
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
        isPass() {
            if (!this.report) return false
            if (this.report.operation) return this.report.operation.result === 'pass'
            if (this.report.assert) return this.report.assert.result === 'pass'
            if (!this.report.action) return false
            let pass = true
            this.report.action.forEach(action => {
                const part = action.operation ? action.operation : action.assert
                if (part && part.result !== 'pass') pass = false
            })
            return pass
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
        isFail() {
            if (!this.report) return false
            if (this.report.operation) return this.report.operation.result === 'fail'
            if (this.report.assert) return this.report.assert.result === 'fail'
            if (!this.report.action) return false
            let fail = false
            this.report.action.forEach(action => {
                const part = action.operation ? action.operation : action.assert
                if (part && part.result === 'fail') fail = true
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
            if (!this.report || !this.report.action) return false
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
