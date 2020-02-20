export default {
    methods: {
        scanForUnusedFixtures(script) {
            let unusedFixtures = []
            if (!script)
                return unusedFixtures
            const usedFixtures = this.scanForUsedFixtures(script)
            const declaredFixtures = this.scanForDeclaredFixtures(script)
            declaredFixtures.forEach(fixture => {
                if (!usedFixtures.includes(fixture))
                    unusedFixtures.push(fixture)
            })
            return unusedFixtures
        },
        scanForDeclaredFixtures(script) {
            let fixtures = []
            if (script.fixture) {
                script.fixture.forEach(fix => {
                    if (fix.id)
                        fixtures.push(fix.id)
                })
            }
            return fixtures
        },
        scanForUsedFixtures(script) {
            let fixtures = []
            if (script.variable) {
                script.variable.forEach(v => {
                    if (v.sourceId)
                        fixtures.push(v.sourceId)
                })
            }
            if (script.setup)
                fixtures = fixtures.concat(this.scanActionForFixtures(script.setup.action))
            if (script.test)
                fixtures = fixtures.concat(this.scanActionForFixtures(script.test.action))
            if (script.teardown)
                fixtures = fixtures.concat(this.scanActionForFixtures(script.teardown.action))
            return fixtures
        },
        scanActionForFixtures(actions) {
            let fixtures = []
            if (!actions)
                return fixtures
            actions.forEach(action => {
                if (action.operation) {
                    if (action.operation.sourceId)
                        fixtures.push(action.operation.sourceId)
                    if (action.assert) {
                        if (action.assert.sourceId)
                            fixtures.push(action.assert.sourceId)
                    }
                }
            })
            return fixtures
        }
    }
}
