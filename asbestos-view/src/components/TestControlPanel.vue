<template>
    <div>
        <div class="control-panel-item-title" @click="manage()">Tests</div>
        <div v-if="!selectable" class="not-available">Select Channel</div>
        <b-form-select v-else v-model="testCollection" :options="testCollections"></b-form-select>
    </div>
</template>

<script>
    import Vue from 'vue'
    import { BFormSelect } from 'bootstrap-vue'
    Vue.component('b-form-select', BFormSelect)
    import {ENGINE} from '../common/http-common'

    export default {
        data() {
            return {
                testCollection: null,
                testCollections: [],
            }
        },
        methods: {
            manage() {
                if (!this.selectable)
                    return;
                const route = `/session/${this.$store.state.base.session}/channel/${this.$store.state.base.channelId}/collection/${this.testCollection}`
                console.log(`Route to ${route}`)
                this.$router.push(route)
            },
            updateTestCollections () {
                this.testCollections = this.$store.state.base.testCollectionNames
            },
            saveTestCollectionName() {
                this.$store.commit('setTestCollection', this.testCollection)
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
            msg(msg) {
                console.log(msg)
                this.$bvToast.toast(msg, {noCloseButton: true})
            },
            error(err) {
                this.$bvToast.toast(err.message, {noCloseButton: true, title: 'Error'})
                console.log(err)
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
        name: "TestControlPanel"
    }
</script>

<style scoped>
    .not-available {
        color: red;
    }
</style>
