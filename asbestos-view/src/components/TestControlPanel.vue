<template>
    <div>
        <div>Test</div>
        <div v-if="!selectable" class="not-available">Select Channel</div>
        <div v-else>
            <div class="control-panel-item-title" @click="openCollection()">Collection</div>
            <b-form-select v-model="collection" :options="collections"></b-form-select>
            <div class="control-panel-item-title" @click="selectIndividual()">Instance</div>
            <b-form-select v-model="testId" :options="testIds"></b-form-select>
        </div>
    </div>
</template>

<script>
    import Vue from 'vue'
    import { BFormSelect } from 'bootstrap-vue'
    Vue.component('b-form-select', BFormSelect)
    import {ENGINE} from '../common/http-common'
    import errorHandlerMixin from '../mixins/errorHandlerMixin'

    export default {
        data() {
            return {
            }
        },
        methods: {
            openCollection() {
                if (!this.selectable)
                    return;
                if (!this.collection)
                    return;
                const route = `/session/${this.session}/channel/${this.channelId}/collection/${this.collection}`
                console.log(`Route to ${route}`)
                this.$router.push(route)
            },
            selectIndividual() {
                if (!this.selectable)
                    return
                if (!this.collection)
                    return;
                if (!this.testId)
                    return
                const route = `/session/${this.session}/channel/${this.channelId}/collection/${this.collection}/test/${this.testId}`
                this.$router.push(route)
            },
            loadTestCollectionNames() {
                const that = this
                ENGINE.get(`collections`)
                    .then(response => {
                        let theResponse = response.data
                        console.info(`TestEnginePanel: loaded ${theResponse.length} test collections`)
                        this.collections = theResponse.sort()
                    })
                    .catch(function (error) {
                        that.error(error)
                    })
            },
        },
        computed: {
            collection: {
                set(name) {
                    this.$store.commit('setTestCollectionName', name)
                    this.openCollection()
                },
                get() {
                    return this.$store.state.testRunner.currentTestCollectionName
                }
            },
            collections: {
                set(names) {
                    this.$store.commit('setTestCollectionNames', names)
                },
                get() {
                    return this.$store.state.testRunner.testCollectionNames
                }
            },
            session() {
                return this.$store.state.base.session
            },
            channelId() {
                return this.$store.state.base.channelId
            },
            selectable() {
                return this.session !== null && this.channelId !== null
            },
            testId: {
                set(name) {
                    this.$store.commit('setCurrentTest', name)
                },
                get() {
                    return this.$store.state.testRunner.currentTest
                }
            },
            testIds() {
                return this.$store.state.testRunner.testScriptNames
            },

        },
        created() {
            this.loadTestCollectionNames()
        },
        mounted() {

        },
        watch: {
            '$store.state.base.channelId': 'loadTestCollectionNames',
        },
        mixins: [ errorHandlerMixin ],
        name: "TestControlPanel"
    }
</script>

<style scoped>
    .not-available {
        color: red;
    }
</style>
