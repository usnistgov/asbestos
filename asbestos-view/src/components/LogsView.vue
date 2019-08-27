<template>
    <div>
        <div class="tool-title">Logs</div>
        <span v-for="(type, i) in resourceTypes" v-bind:key="i">
            <span class="selectable" @click="loadType(type)">{{ type }}</span>
            <span class="divider"></span>
        </span>
        <div v-for="event in events" :key="event.type">
            <div class="event-type-header solid-boxed">
                <span class="type-label has-cursor" v-if="!event.open">
                    <img id="closed-button" src="../assets/arrow-right.png" @click="open(event)"/>
                </span>
                <span class="type-label has-cursor" v-else>
                    <img id="opened-button" src="../assets/arrow-down.png" @click="close(event)"/>
                </span>
                <span class="type-label">{{ event.type }}</span>
                <span class="divider"></span>
                <span>{{ event.events.length }} events</span>
            </div>
            <div v-if="event.open">
                <div v-for="(eventName, i) in event.events" v-bind:key="i">
                    {{ eventName }}
                </div>
            </div>
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
                events: [], // type => list of events
            }
        },
        methods: {
            open(event) {
                  event.open = true
            },
            close(event) {
                event.open = false
            },
            keys() {
                var theList = []
                this.events.forEach(event => {
                    theList.push(event.type)
                })
            },
            loadType(type) {
                console.log(`selected ${type}`)
                const that = this
                if (type === 'All') {
                    this.resourceTypes.forEach(theType => {
                        if (theType !== 'All')
                            that.loadEvents(theType)
                    })
                } else
                    this.loadEvents(type)
                console.log(`events are ${this.keys()}`)
            },
            loadEvents(type) {
                console.log(`LogsView: loading ${type}...`)
                this.events.length = 0
                const that = this
                LOG.get(`${this.sessionId}/${this.channelId}/${type}`)
                    .then(response => {
                        let theResponse = response.data
                        console.log(`...${theResponse.length} events`)
                        this.events.push({type: type, open: false, events: theResponse.sort().reverse()})
                        console.log(`events are ${this.keys()}`)
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
    .type-label {
        font-weight: bold;
        float: left;
    }
    .event-type-header  {
        padding: 2px;
    }
</style>
