<template>
    <div>
        <span class="tool-title">Tests for Channel {{ testCollection }}</span>
        <span class="divider"></span>

        <img id="reload" class="selectable" @click="loadTestScriptNames()" src="../assets/reload.png"/>
        <span class="divider"></span>

        <div v-for="(name, i) in testScriptNames"
             :key="name + i">
            <div >
                <div @click="selectTest(name)">
                    <div v-bind:class="{ pass: isPass, fail: isFail, 'not-run': isNotRun   }">
                        <span v-if="isPass">
                            <img src="../assets/checked.png" class="right">
                        </span>
                        <span v-if="isFail">
                            <img src="../assets/error.png" class="right">
                        </span>
                        <span v-if="isNotRun">
                            <img src="../assets/blank-circle.png" class="right">
                        </span>
                        <img src="../assets/press-play-button.png" class="right">
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
                testScriptNames: [],
                selected: null,  // name
            }
        },
        methods: {
            selectTest(name) {
                if (this.selected === name) {
                    this.selected = null
                    const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}`
                    console.log(`route to ${route}`)
                    this.$router.push(route)
                    return
                }
                this.selected = name
                const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${name}`
                console.log(`route to ${route}`)
                this.$router.push(route)
            },
            loadTestScriptNames() {
                const that = this
                ENGINE.get(`collection/${this.testCollection}`)
                    .then(response => {
                        let theResponse = response.data
                        console.info(`TestEnginePanel: loaded ${theResponse.length} test script names`)
                        this.testScriptNames = theResponse
                    })
                    .catch(function (error) {
                        that.error(error)
                    })
            },
            isPass() {
                return this.current.run === true && this.current.pass === true
            },
            isFail() {
                return this.current.run === true && this.current.pass === false
            },
            isNotRun() {
                return this.current.run === false
            },
        },
        computed: {
            current() {
                return this.$store.state.base.testCollectionDetails.find(item => {
                    return item.name === this.testId
                })
            },
        },
        created() {
            this.loadTestScriptNames()
        },
        mounted() {

        },
        watch: {

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
