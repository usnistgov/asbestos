<template>
    <div>
<!--        <div>Test</div>-->
        <div v-if="!selectable" class="not-available">Select FHIR Server</div>
        <div v-else>
            <div>
                <div class="control-panel-item-title" @click="openCollection()">Test Collections</div>
                <span class="selectable" @click="openCollection()">View Selected</span>
                <span class="divider"></span>
                <img id="reload" class="selectable" @click="reload()" src="../../assets/reload.png"/>
                <br />

<!--                <input type="radio" id="client" value="Client" v-model="testType">-->
<!--                <label for="client">Client</label>-->
<!--                <input type="radio" id="server" value="Server" v-model="testType">-->
<!--                <label for="server">Server</label>-->
            </div>
            <select v-model="collection" v-bind:size="collectionDisplaySize" class="control-panel-font">
                <option disabled>Client:</option>
                <option v-for="(coll, collectioni) in clientCollections"
                        v-bind:value="coll"
                        :key="coll + collectioni"
                >
                    {{ coll }}
                </option>
                <option disabled>Server:</option>
                <option v-for="(coll, collectioni) in serverCollections"
                        v-bind:value="coll"
                        :key="coll + collectioni"
                >
                    {{ coll }}
                </option>
            </select>
            <!--            <b-form-select class="control-panel-font" v-model="collection" :options="collections"></b-form-select>-->
        </div>
    </div>
</template>

<script>
    import Vue from 'vue'
    import { BFormSelect } from 'bootstrap-vue'
    Vue.component('b-form-select', BFormSelect)
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'

    export default {
        data() {
            return {
                collection: null,
                testType: "Server", // Client or Server
            }
        },
        methods: {
            reload() {
                this.$store.dispatch('loadTestCollectionNames')
            },
            vuexCollectionUpdated() {
                if (this.collection !== this.$store.state.testRunner.currentTestCollectionName) {
                    this.collection = this.$store.state.testRunner.currentTestCollectionName
                    this.openCollection()
                }
            },
            localCollectionUpdated() {
//                if (this.collection !== this.$store.state.testRunner.currentTestCollectionName)
                    this.openCollection()
            },
            openCollection() {
                if (!this.selectable)
                    return;
                this.$store.commit('setTestCollectionName', this.collection)
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
            collectionDisplaySize() {
                return this.clientCollections.length + this.serverCollections.length + 2
            },
            client() {
                return this.testType === 'Client'
            },
            // collection: {
            //     set(name) {
            //         this.$store.commit('setTestCollectionName', name)
            //         this.openCollection()
            //     },
            //     get() {
            //         return this.$store.state.testRunner.currentTestCollectionName
            //     }
            // },
            collections: {
                get() {
                    return (this.client)
                    ? this.$store.state.testRunner.clientTestCollectionNames
                        : this.$store.state.testRunner.serverTestCollectionNames
                }
            },
            clientCollections() {
                return this.$store.state.testRunner.clientTestCollectionNames
            },
            serverCollections() {
                return this.$store.state.testRunner.serverTestCollectionNames
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
            '$store.state.testRunner.currentTestCollectionName': 'vuexCollectionUpdated',
            'collection': 'localCollectionUpdated',
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
