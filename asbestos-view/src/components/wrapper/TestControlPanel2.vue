<template>
    <div>
        <div v-if="!selectable" class="not-available">Select FHIR Server</div>
        <div v-else>
            <div>
                <span class="control-panel-item-title" @click="openCollection()">Test Collections</span>
                <img id="reload" class="selectable" @click="reload()" src="../../assets/reload.png"/>
                <br />
            </div>

            <div class="disabled">
                Client:
            </div>
            <div v-for="(coll, collectioni) in clientCollections"
                 :key="coll + collectioni" class="selectable">
                <span v-bind:class="{active: coll === collection}" @click="openTheCollection(coll)">{{ coll }}</span>
            </div>

            <div class="disabled">
                Server:
            </div>
            <div v-for="(coll, collectioni) in serverCollections"
                 :key="coll + collectioni" class="selectable">
                <span v-bind:class="{active: coll === collection}" @click="openTheCollection(coll)">{{ coll }}</span>
            </div>

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
                if (this.$store.state.testRunner.currentTestCollectionName === null)
                    return;
                if (this.$store.state.testRunner.autoRoute && this.collection !== this.$store.state.testRunner.currentTestCollectionName) {
                    this.collection = this.$store.state.testRunner.currentTestCollectionName
                    this.openCollection()
                }
            },
            localCollectionUpdated() {
//                if (this.collection !== this.$store.state.testRunner.currentTestCollectionName)
                    this.openCollection()
            },
            openTheCollection(collection) {
                if (!this.selectable)
                    return;
                this.$store.commit('setTestCollectionName', collection)
                if (!collection)
                    return;
                this.collection = collection
                const route = `/session/${this.session}/channel/${this.channelId}/collection/${collection}`
                this.$router.push(route)
            },
            openCollection() {
                if (!this.selectable)
                    return;
                this.$store.commit('setTestCollectionName', this.collection)
                if (!this.collection)
                    return;
                const route = `/session/${this.session}/channel/${this.channelId}/collection/${this.collection}`
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
    .active {
        background-color: lightgray;
    }
    .disabled {
        color: lightgray;
    }
    .not-available {
        color: red;
    }
</style>
