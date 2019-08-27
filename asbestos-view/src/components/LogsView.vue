<template>
    <div>
        <div class="tool-title">Logs</div>
        <span v-for="type in resourceTypes" v-bind:key="type">
            <span class="selectable" @click="select(type)">{{ type }}</span>
            <span class="divider"></span>
        </span>
    </div>
</template>

<script>
    import Vue from 'vue'
    import { TooltipPlugin, ToastPlugin } from 'bootstrap-vue'
    Vue.use(TooltipPlugin)
    import {LOG} from '../common/http-common'
    Vue.use(ToastPlugin)

    export default {
        data() {
            return {
                resourceTypes: [],
                events: [], // each element is array of event names
            }
        },
        methods: {
            select(type) {
                console.log(`selected ${type}`)
            },
            getEvents(type) {
                const that = this
                LOG.get(`${this.sessionId}/${this.channelId}/${type}`)
                    .then(response => {
                        let theResponse = response.data
                        this.resourceTypes =  theResponse.sort()
                    })
                    .catch(function (error) {
                        that.error(error)
                    })
            },
            getResourceTypes() {
                const that = this
                LOG.get(`${this.sessionId}/${this.channelId}`)
                    .then(response => {
                        let theResponse = response.data
                        theResponse.push('All')
                        this.resourceTypes =  theResponse.sort()
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
        created() {
            console.log('LogView created')
        },
        mounted() {
            console.log('LogView mounted')
            this.getResourceTypes()
        },
        watch: {

        },
        props: [
            'sessionId', 'channelId'
        ],
        name: "LogsView"
    }
</script>

<style scoped>

</style>
