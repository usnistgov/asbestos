<template>
    <div>
        <div>
            <span class="tool-title">Logs</span>
            <span class="divider"></span>
            <img id="reload" class="selectable" @click="loadEventSummaries()" src="../assets/reload.png"/>
        </div>

        Logs exists for these resource types

        <div v-for="(type, i) in resourceTypes"
              v-bind:key="i">
            <span class="selectable"
                  @click="typeSelected(type)">{{ type }}</span>
            <span class="divider"></span>
        </div>
    </div>
</template>

<script>
    import {LOG} from '../common/http-common'

    export default {
        data() {
            return {
                resourceTypes: [],
                eventSummaries: [],  // list { eventName: xx, resourceType: yy, verb: GET|POST, status: true|false }
            }
        },
        methods: {
            typeSelected(type) {
               this.$router.push(`/session/${this.sessionId}/channel/${this.channelId}/${type}`)
            },
            loadEventSummaries() {
                LOG.get(`${this.sessionId}/${this.channelId}`, {
                    params: {
                        summaries: 'true'
                    }
                })
                    .then(response => {
                        this.eventSummaries = response.data
                        let types = []
                        this.eventSummaries.forEach(summary => {
                            if (!types.includes(summary.resourceType))
                                types.push(summary.resourceType)
                        })
                        types.push('All')
                        this.resourceTypes = types.sort()
                        console.log(`loaded ${response.data.length} summaries and ${types.length} types`)
                    })
                    .catch(error => {
                        this.error(error)
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
            this.loadEventSummaries()
        },
        props: [
            'sessionId', 'channelId'
        ],
        name: "ChannelLogList"
    }

</script>

<style scoped>

</style>
