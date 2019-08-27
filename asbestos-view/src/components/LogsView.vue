<template>
    <div>
        <div class="tool-title">Logs</div>
        <span v-for="(type, i) in resourceTypes" v-bind:key="i">
            <span class="selectable" @click="loadEvents(type)">{{ type }}</span>
            <span class="divider"></span>
        </span>
        <div v-for="event in events" :key="event.id">
            {{ event.id }} - {{ event.type }}
        </div>
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
                events: [], // list { event: theEvent, id: theId, type: theType }
            }
        },
        methods: {
            open(event) {
                  event.open = true
            },
            close(event) {
                event.open = false
            },
            async loadEvents(type) {
                console.log(`selected ${type}`)
                const that = this
                if (type === 'All') {
                    let idMap = []
                    this.resourceTypes.forEach(theType => {
                        if (theType !== 'All') {
                            const ids = that.loadEventIds(theType)
                            ids.forEach(id => {
                                const event = this.loadEvent(type, id)
                                const eventObj = { event: event, id: id, type: theType }
                                idMap.push(eventObj)
                            })
                        }
                    })
                    this.events = idMap
                } else {
                    const ids = await this.loadEventIds(type)  // => this.eventIds
                    this.events = ids.map(id => {
                        const thisEvent = this.loadEvent(type, id)  // => this.thisEvent
                        return { event: thisEvent, id: id, type: type }
                    })
                }
            },
            async loadEvent(theType, theId) {
                console.log(`loadEvent ${this.sessionId}/${this.channelId}/${theType}/${theId}`)
                try {
                    return await LOG.get(`${this.sessionId}/${this.channelId}/${theType}/${theId}`)
                } catch (error) {
                    this.error(error)
                }
            },
            async loadEventIds (type) {
                console.log(`loadEventIds ${type} ...`)
                try {
                    return await LOG.get(`${this.sessionId}/${this.channelId}/${type}`)
                } catch (error) {
                    this.error(error)
                }
            },
            loadResourceTypes() {
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
            this.loadResourceTypes()
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
    .type-label {
        font-weight: bold;
        float: left;
    }
    .event-type-header  {
        padding: 2px;
        background-color: lightGray;
    }
</style>
