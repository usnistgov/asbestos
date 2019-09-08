<template>
    <div>
        <div class="control-panel-item-title" @click="manage()">Test</div>
        <div v-if="!selectable" class="not-available">Select Channel</div>
        <div v-else>
            <div class="control-panel-item-title" @click="manage()">Collection</div>
            <b-form-select v-model="testCollectionName" :options="testCollections"></b-form-select>
            <div class="control-panel-item-title" @click="selectIndividual()">Individual</div>
            <b-form-select v-model="individual" :options="$store.state.base.testIds"></b-form-select>
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
                testCollectionName: null,
                testCollections: [],
                individual: null,
            }
        },
        methods: {
            manage() {
                if (!this.selectable)
                    return;
                if (!this.testCollectionName)
                    return;
                const route = `/session/${this.$store.state.base.session}/channel/${this.$store.state.base.channelId}/collection/${this.testCollectionName}`
                console.log(`Route to ${route}`)
                this.$router.push(route)
            },
            selectIndividual() {

            },
            updateTestCollections () {
                this.testCollections = this.$store.state.base.testCollectionNames
            },
            saveTestCollectionName() {
                this.$store.commit('setTestCollectionName', this.testCollectionName)
            },
            loadTestCollectionNames() {
                const that = this
                ENGINE.get(`collections`)
                    .then(response => {
                        let theResponse = response.data
                        console.info(`TestEnginePanel: loaded ${theResponse.length} test collections`)
                        this.$store.commit('setTestCollectionNames', theResponse.sort())
                    })
                    .catch(function (error) {
                        that.error(error)
                    })
            },
        },
        computed: {
            selectable() {
                return this.$store.state.base.session !== null && this.$store.state.base.channelId !== null
            }
        },
        created() {

        },
        mounted() {

        },
        watch: {
            '$store.state.base.channelId': 'loadTestCollectionNames',
            '$store.state.base.testCollectionNames': 'updateTestCollections',
            'testCollection': 'saveTestCollectionName',
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
