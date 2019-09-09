<template>
    <div>
        <span class="tool-title">Tests for Collection {{ testCollection }}</span>
        <span class="divider"></span>

        <img id="reload" class="selectable" @click="reload()" src="../assets/reload.png"/>
        <span class="divider"></span>

        <div v-for="(name, i) in $store.state.testRunner.testScriptNames"
             :key="name + i">
            <div >
                <div @click="selectTest(name)">
                    <div v-bind:class="{ pass: pass(name), fail: fail(name), 'not-run': notRun(name)   }">
                        <div v-if="pass(name)">
                            <img src="../assets/checked.png" class="right">
                        </div>
                        <div v-else-if="fail(name)">
                            <img src="../assets/error.png" class="right">
                        </div>
                        <div v-else>
                            <img src="../assets/blank-circle.png" class="right">
                        </div>
                        <img src="../assets/press-play-button.png" class="right" @click.stop="doRun(name)">
                        {{ name }}
                    </div>
                </div>
                <div v-if="selected === name">
                    <router-view></router-view>
                </div>
            </div>
        </div>

    </div>
</template>

<script>
    import {ENGINE} from '../common/http-common'
    import errorHandlerMixin from '../mixins/errorHandlerMixin'

    export default {

        data() {
            return {

            }
        },
        methods: {
            doRun(testName) {
 //               console.log(`run ${testName}`)
                const that = this
                ENGINE.post(`testrun/${this.sessionId}__${this.channelId}/${this.testCollection}/${testName}`)
                    .then(response => {
//                        console.log(`response => ${response.data.result}`)
                        this.$store.commit('addTestReport', { name: testName, report: response.data } )
                        this.$router.replace(`/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${testName}`)
                    })
                    .catch(error => {
                        that.error(error)
                    })
            },
            selectTest(name) {
                if (this.selected === name)  { // unselect
                    this.$store.commit('setCurrentTest', null)
                    const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}`
 //                   console.log(`route to ${route}`)
                    this.$router.push(route)
                    return
                }
                this.$store.commit('setCurrentTest', name)
                const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${name}`
//                console.log(`route to ${route}`)
                this.$router.push(route)
            },


            reload() {
                console.log('TestList reload()')
                this.$store.dispatch('loadTestScriptNames')
               // this.$store.dispatch('loadReports')
            },
            pass(testName) {
                return this.$store.state.testRunner.testReports[testName] !== undefined && this.$store.state.testRunner.testReports[testName].result === 'pass'
            },
            fail(testName) {
                return this.$store.state.testRunner.testReports[testName] !== undefined && this.$store.state.testRunner.testReports[testName].result === 'fail'
            },
            notRun(testName) {
                return this.$store.state.testRunner.testReports[testName] === undefined
            },
        },
        computed: {
            current() {
                return this.$store.state.base.testCollectionDetails.find(item => {
                    return item.name === this.testId
                })
            },
            selected() {
                return this.$store.state.testRunner.currentTest
            },
            loadTestScriptNames() {
                return this.$store.dispatch('loadReports')
            },
            testScriptNames() {
                return Object.keys(this.$store.state.testRunner.testReports).sort()
            }
        },
        created() {
            this.reload()
        },
        mounted() {

        },
        watch: {
            'testCollection': 'loadTestScriptNames',
        },
        mixins: [ errorHandlerMixin ],
        name: "TestList",
        props: [
            'sessionId', 'channelId', 'testCollection',
        ]
    }
</script>

<style scoped>
    .banner-color {
        background-color: lightgray;
        text-align: left;
    }
    .pass {
        background-color: lightgreen;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
    }
    .fail {
        background-color: indianred;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
    }
    .not-run {
        background-color: lightgray;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
    }
    .right {
        text-align: right;
    }
</style>
