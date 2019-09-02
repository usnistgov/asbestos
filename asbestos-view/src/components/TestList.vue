<template>
    <div>
        <span class="tool-title">Tests for Channel {{ testCollection }}</span>
        <span class="divider"></span>

        <img id="reload" class="selectable" @click="loadTestScriptNames()" src="../assets/reload.png"/>
        <span class="divider"></span>

        <div v-for="(name, i) in testScriptNames"
             :key="name + i">
            <div >
                <div class="summary-label boxed has-cursor banner-color"
                     @click="selectTest(name)">
                    {{ name }}
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
                selected: null,
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
        },
        computed: {

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
</style>
