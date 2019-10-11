<template>
    <div>
<!--        <div>Test</div>-->
        <div v-if="!selectable" class="not-available">Select FHIR Server</div>
        <div v-else>
            <div>
                <span class="control-panel-item-title" @click="openCollection()">Test Collection</span>
                <span class="divider"></span>
                <img id="reload" class="selectable" @click="reload()" src="../assets/reload.png"/>
            </div>
            <b-form-select v-model="collection" :options="collections"></b-form-select>

            <div v-if="client">(Client Tests)</div>
            <div v-else>
                <div v-if="collection">
                    (Server Tests)
                </div>
            </div>
<!--            <div class="control-panel-item-title" @click="selectIndividual()">Tests</div>-->
<!--            <b-form-select v-model="testId" :options="testIds"></b-form-select>-->
        </div>
    </div>
</template>

<script>
    import Vue from 'vue'
    import { BFormSelect } from 'bootstrap-vue'
    Vue.component('b-form-select', BFormSelect)
    import errorHandlerMixin from '../mixins/errorHandlerMixin'

    export default {
        data() {
            return {
            }
        },
        methods: {
            reload() {
                this.$store.dispatch('loadTestCollectionNames')
            },
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
                if (!this.$store.state.testRunner.currentTestCollectionName)
                    return;
                if (!this.$store.state.testRunner.currentTest)
                    return
                const route = `/session/${this.session}/channel/${this.channelId}/collection/${this.collection}/test/${this.testId}`
                this.$router.push(route)
            },
        },
        computed: {
            client() {
                return this.$store.state.testRunner.isClientTest
            },
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
                return this.$store.state.base.session !== null && this.$store.state.base.channelId !== null
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
            this.reload()
        },
        mounted() {

        },
        watch: {
            '$store.state.base.channelId': 'reload',
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
