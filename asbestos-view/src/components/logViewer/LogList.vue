<template>
    <div>
        <div>
            <span class="tool-title">Events for Channel {{ channelName }}</span>
            <span class="divider"></span>

            <img id="reload" class="selectable" @click="loadEventSummaries()" src="../../assets/reload.png" title="Refresh Events"/>
            <div>
                <img id="left" class="selectable" @click="pageLeft()" src="../../assets/left-arrow.png" title="Events"/>&nbsp;
                {{'Page ' + currentPage + ' of ' + totalPageCount}}
                <img id="right" class="selectable" @click="pageRight()" src="../../assets/right-arrow.png" title="Events"/>
            </div>
            <div class="divider"></div>
        </div>
        <div>
            Source IP addr filter:
            <select v-model="selectedIP" v-bind:size="1">
                <option v-for="(coll, colli) in ipAddresses"
                        v-bind:value="coll"
                        :key="coll + colli"
                >
                    {{ coll }}
                </option>
            </select>
        </div>

        <template v-if="isLoading && currentPage===1"><p class="loading">Loading...</p></template>
        <template v-else>
            <template v-if="!needRefresh">
                <div class="vdivider"></div>

                <div v-for="(eventSummary, i) in eventSummaries"
                     :key="eventSummary.eventName + i">
                    <div v-if="selectedIP === 'all' || eventSummary.ipAddr === selectedIP">
                        <div class="summary-label boxed has-cursor left"
                             @click="selectSummary(eventSummary)">
                            {{ eventAsDate(eventSummary.eventName) }} - {{ eventSummary.ipAddr }} - {{ eventSummary.verb}} {{ eventSummary.resourceType }} - {{ eventSummary.status ? 'Ok' : 'Error' }}
                        </div>

                    </div>
                </div>
                <div v-if="eventSummaries.length === 0">
                    <p>No events. Click refresh to reload.</p>
                </div>
                <div v-else>
                    <img id="left2" class="selectable" @click="pageLeft()" src="../../assets/left-arrow.png" title="Events"/>&nbsp;
                    {{'Page ' + currentPage + ' of ' + totalPageCount}}
                    <img id="right2" class="selectable" @click="pageRight()" src="../../assets/right-arrow.png" title="Events"/>
                </div>
            </template>
            <template v-else>
               <p>Please click the Refresh Events image icon.</p>
            </template>

        </template>


    </div>
</template>

<script>
    import eventMixin from '../../mixins/eventMixin'
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'

    export default {
        data() {
            return {
                eventSummariesByType: [],
                selectedEventName: null,
                selectedEvent: null,
                selectedTask: 0,
                selectedIP: "all",
                isLoading: false,
                needRefresh: false,
                currentPage: 1,
                totalPageCount : 1,
                itemsPerPage: 50,
            }
        },
        methods: {
            selectSummary(summary) {
                //this.$store.commit('setEventSummaries', this.eventSummaries)
                this.$router.push(`/session/${this.sessionId}/channel/${this.channelName}/lognav/${summary.eventName}?clientIP=${summary.ipAddr}`)
            },
            pageLeft() {
                // console.log('currentPage: ' + this.currentPage)
                if (this.currentPage > 1) {
                    this.currentPage--
                    this.loadEventSummaries()
                }
            },
            pageRight() {
                // console.log('currentPage: ' + this.currentPage)
                // console.log(JSON.stringify(this.$store.state.log.eventSummaries[0]))
                if (this.currentPage < this.totalPageCount) {
                    this.currentPage++
                    this.loadEventSummaries()
                }
            },
            loadEventSummaries() {
                this.needRefresh = false
                this.isLoading = true
                this.$store.dispatch('loadEventSummaries', {session: this.sessionId, channel: this.channelName, itemsPerPage: this.itemsPerPage, page: this.currentPage})
                    .then(() => {
                        this.isLoading = false
                        if ('totalPageableItems' in this.$store.state.log.eventSummaries[0]) {
                            const totalPageableItems = this.$store.state.log.eventSummaries[0].totalPageableItems
                            this.totalPageCount = totalPageableItems
                        }

                    })
            },
            displayInstruction() {
               this.needRefresh = true
            }
        },
        created() {
            this.loadEventSummaries()
        },
        computed: {
            eventSummaries() {   // list { eventName: xx, resourceType: yy, verb: GET|POST, status: true|false }
                return this.$store.state.log.eventSummaries
            },
            ipAddresses() {
                let addrs = this.$store.getters.ipAddresses
                addrs.push("all")
                return addrs
            },
        },
        watch: {
            'resourceType': 'loadEventSummaries',
            'channelName': 'displayInstruction',
        },
        props: [
            'resourceType',  'sessionId', 'channelName',
        ],
        mixins: [eventMixin, errorHandlerMixin],
        name: "LogList"
    }
</script>

<style scoped>

</style>
