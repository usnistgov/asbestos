<template>
    <div>
        <div>
            <span class="tool-title">Events for Channel {{ channelName }}</span>
            <span class="divider"></span>
            <img id="reload" class="selectable" @click="loadEventSummaries()" src="../../assets/reload.png" title="Refresh Events"/>
            <div>
                <img id="left" class="selectable" @click="pageLeft()" src="../../assets/left-arrow.png" title="Events"/>&nbsp;
                <label for="pageNumSelect">Page</label>&nbsp;
                <select id="pageNumSelect" v-model="pageNumValueHandler" v-bind:size="1">
                    <option v-for="(pgNum, idx) in totalPageCount"
                            v-bind:value="(pgNum)"
                            :key="pgNum + idx"
                    >
                        {{ pgNum }}
                    </option>
                </select>
                &nbsp; of &nbsp; <span class="selectable" @click="gotoLastPage" title="Go to last page"> {{ totalPageCount}}</span>&nbsp;
                <img id="right" class="selectable" @click="pageRight()" src="../../assets/right-arrow.png" title="Events"/>
            </div>
            <span v-show="!$store.state.log.loaded" class="yellowBkgText">Loading...</span>
            <div class="divider"></div>
        </div>
        <div>
            <label for="selectedIPSelect">Source IP addr filter:</label>&nbsp;
            <select id="selectedIPSelect" v-model="selectedIP" v-bind:size="1">
                <option v-for="(coll, colli) in ipAddresses"
                        v-bind:value="coll"
                        :key="coll + colli"
                >
                    {{ coll }}
                </option>
            </select>.
            &nbsp;
            <label for="pageSizeSelect">Page size:</label>&nbsp;
            <select id="pageSizeSelect" v-model="pageSizeValueHandler" v-bind:size="1">
                <option v-for="(pgSz, idx) in pageSizeValues"
                        v-bind:value="pgSz"
                        :key="pgSz + idx"
                >
                    {{ pgSz }}
                </option>
            </select>.
        </div>

        <template>
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
                previousPageSize: 25,
                selectedPageSize: 25,
                pageSizeValues: [5,10,25,50,100],
                isLoading: false,
                needRefresh: false,
                currentPage: 1,
                totalPageCount : 1,
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
                    this.previousPageSize = -1
                    this.loadEventSummaries()
                }
            },
            pageRight() {
                // console.log('currentPage: ' + this.currentPage)
                // console.log(JSON.stringify(this.$store.state.log.eventSummaries[0]))
                if (this.currentPage < this.totalPageCount) {
                    this.currentPage++
                    this.previousPageSize = -1
                    this.loadEventSummaries()
                }
            },
            gotoPage() {
                if (this.currentPage != 1) {
                    this.currentPage = 1
                    this.previousPageSize = -1
                    this.loadEventSummaries()
                }
            },
            gotoLastPage() {
                if (this.currentPage != this.totalPageCount) {
                    this.currentPage = this.totalPageCount
                    this.previousPageSize = -1
                    this.loadEventSummaries()
                }
            },
            loadEventSummaries() {
                this.needRefresh = false
                this.isLoading = true
                let paramsObj = {session: this.sessionId, channel: this.channelName, itemsPerPage: this.selectedPageSize, page: this.currentPage, previousPageSize: this.previousPageSize}
                this.$store.dispatch('loadEventSummaries', paramsObj)
                    .then(() => {
                        this.isLoading = false
                        if ('totalPageableItems' in this.$store.state.log.eventSummaries[0]) {
                            const totalPageableItems = this.$store.state.log.eventSummaries[0].totalPageableItems
                            this.totalPageCount = totalPageableItems
                        }
                        if ('newPageNum' in this.$store.state.log.eventSummaries[0]) {
                            const newPageNum = this.$store.state.log.eventSummaries[0].newPageNum
                            this.currentPage = newPageNum
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
            pageSizeValueHandler: {
                set(val) {
                    if (val === '' || val === undefined)
                        return;
                    this.previousPageSize = this.selectedPageSize
                    this.selectedPageSize = val
                    this.loadEventSummaries()
                },
                get() {
                    return this.selectedPageSize
                }
            },
            pageNumValueHandler: {
                set(val) {
                    if (val === '' || val === undefined)
                        return;
                    this.previousPageSize = this.selectedPageSize
                    this.currentPage = val
                    this.loadEventSummaries()
                },
                get() {
                    return this.currentPage
                }
            }
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
    .yellowBkgText {
        color: black;
        background-color: yellow;
        font-size: smaller;
    }

</style>
