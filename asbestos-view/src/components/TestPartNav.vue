<template>
    <div>
        <img v-if="open" src="../assets/arrow-down.png" @click="closeIt">
        <img v-else src="../assets/arrow-right.png" @click="openIt">
        {{ capitalize(type) }}
        <img src="../assets/add-button.png" @click="addNewThingToTest">
        <span style="color:blue; font-size: 0.8em"> ({{ count }})</span>
        <div v-if="open">
            <div v-for="element in elements(testId, type)" :key="element.id">
                <router-link class="element-nav" v-bind:to="variableUrl(element.id)">
                    {{ element.name }}
                </router-link>
            </div>
        </div>
    </div>
</template>
<script>
    import {newTestPart} from "../types/test";

    export default {
        data() {
            return {
                openPartIds: [],
                open: false
            }
        },
        props: [
            'testId',  // id of test
            'type' // element of test - head, variable, setup,  teardown
            ],
        computed: {
            count() {
                return this.elements(this.testId, this.type).length
            }
        },
        methods: {
            variableUrl(id) {
                return '/test/' + this.testId + '/variable/' + id
            },
            addNewThingToTest() {
                // The type of thing is this.type
                // this is hung off UI button

                // assign unique id within this test
                const testIndex = this.assignTestIndex()
                const vars = this.$store.state.base.tests[testIndex][this.type]

                let idi = 1
                for (let theVar of vars) {
                    const theVarId = parseInt(theVar.id)
                    if (theVarId > idi) {
                        idi = theVarId
                    }
                }
                const id = (idi + 1).toString()

                // name follows id
                const name = '#undefined' + id


                const testPart = newTestPart(this.noEndS(this.type))
                testPart.id = id
                testPart.name = name
                //testPart.testId = this.testId

                // store has mutations for installTestPART where
                // PART is variables | fixtures | ...
                // calculate PART from this.type
                const part = this.noEndS(this.capitalize(this.type))
                this.$store.commit('installTest' + part, { testId: this.testId, part: testPart })
            },
            noEndS(it) {
                if (it.substr(-1) === 's') {
                    it = it.slice(0, -1)
                }
                return it
            },
            capitalize(it) {
                return it.charAt(0).toUpperCase() + it.slice(1)
            },
            openIt() {
                this.open = true
            },
            closeIt() {
                this.open = false
            },
            elements(testId, type) {
                return this.elementsOfTest(testId, type )
            },
            assignTestIndex() {
                const theTestId = this.testId
                return this.$store.state.base.tests.findIndex(function (test) {
                    return test.id === theTestId
                })
            },
            elementsOfTest(testId, type) {
                // retuns array of fixtures|variables|... based on type
                let ids = []
                this.$store.state.base.tests.forEach (function(test) {
                    ids.push(test.id)
                })
                const testIndex = this.$store.state.base.tests.findIndex(function (test) {
                    return test.id === testId
                })
                if (testIndex === -1) { throw `Cannot find test id ${testId} in TestPartNav.elementsOfTest` }
                const test = this.$store.state.base.tests[testIndex]
                switch (type) {
                    case 'heads': return test.heads
                    case 'fixtures': return test.fixtures
                    case 'variables': return test.variables
                    case 'setups': return test.setups
                    case 'tests': return test.tests
                    case 'teardowns': return test.teardowns
                    default: return null
                }
            }
        }
    }
</script>
<style scoped>
    .element-nav {
        position: relative;
        left: 15px;
    }
    .router-link-active {
        background-color: lightblue;
    }
</style>
