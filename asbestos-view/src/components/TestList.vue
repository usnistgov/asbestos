<template>
    <div>
      <div v-for="(name, i) in $store.state.testRunner.testReportNames"
         :key="name + i">
        <div >
            <div @click="selectTest(name)">
                <div v-bind:class="{ pass: status[name] === 'pass', fail: status[name] === 'fail', error: status[name] === 'error', 'not-run': status[name] === 'undefined' }">
                    <div v-if="status[name] === 'pass'">
                        <img src="../assets/checked.png" class="right">
                    </div>
                    <div v-else-if="status[name] === 'fail' || status[name] === 'error'">
                        <img src="../assets/error.png" class="right">
                    </div>
                    <div v-else>
                        <img src="../assets/blank-circle.png" class="right">
                    </div>
                    {{ name }}  --  {{ time[name] }}
                </div>
            </div>
<!--            <div v-if="selected === name">-->
<!--                <router-view></router-view>-->
<!--            </div>-->
        </div>
      </div>
    </div>
</template>

<script>
    export default {
        data() {
            return {
                status: [],   // testName => undefined, 'pass', 'fail', 'error'
                time: [],
            }
        },
        methods: {
            selectTest(name) {
                if (this.selected === name)  { // unselect
                    this.$store.commit('setCurrentTest', null)
                    const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}`
                    this.$router.push(route)
                    return
                }
                this.$store.commit('setCurrentTest', name)
                const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${name}`
                this.$router.push(route)
            },
            updateReportStatuses() {  // this needs rework
                let status = []
                let time = []
                this.allTestScriptNames().forEach(testName => {
                    if (this.testReport(testName) === undefined) {
                        status[testName] = 'undefined'
                    } else {
                        status[testName] = this.testReport(testName).result  // 'pass', 'fail', 'error'
                        time[testName] = this.testReport(testName).issued
                    }
                    this.status = status
                    this.time = time
                })
            },
            allTestScriptNames() {
                return this.$store.state.testRunner.testReportNames
            },
        },
        computed: {

        },
        name: "TestList"
    }
</script>

<style scoped>

</style>
