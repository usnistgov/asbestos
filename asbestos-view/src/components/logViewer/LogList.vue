<template>
    <div>
        <div>
            <span class="tool-title">
            <template v-if="!needRefresh && totalEventCount > 0">
                {{totalEventCount.toLocaleString("en-US")}}
            </template>
            Events for Channel {{ channelName }}
            </span>
            <span class="divider"></span>
            <img id="reload" class="selectable" @click="loadEventSummaries()" src="../../assets/reload.png" title="Refresh Events"/>
            <template v-if="!needRefresh && totalEventCount > 0">
             <div>

                 <img id="left" class="selectable" @click="pageLeft()" src="../../assets/left-arrow.png" title="Events"/>&nbsp;
                                Page {{currentPage}}
                &nbsp; of &nbsp; <span class="selectable" @click="gotoLastPage" title="Go to last page"> {{ totalPageCount}}</span>&nbsp;
                <img id="right" class="selectable" @click="pageRight()" src="../../assets/right-arrow.png" title="Events"/>
            </div>
            <div v-if="totalPageCount > 1" class="pageNumLayoutMain">
                <div class="leftPad"></div>
                <div class="centerContent">
                    <div class="pageNumNavMain">
                        <div class="pageItem" v-for="(pgNum, idx) in totalPageCount"
                             :key="pgNum + idx"
                        ><span :class="{'currentPageNum':currentPage===pgNum,'selectable':currentPage!==pgNum}" @click="gotoPage(pgNum)">{{pgNum}}</span></div>
                    </div>
                </div>
                <div class="rightPad"></div>
            </div>
            </template>
            <template v-else><div/></template>
            <span v-show="!$store.state.log.loaded" class="loadingBkgText">Loading...</span>
            <div class="divider"></div>
        </div>
        <template v-if="!needRefresh && totalEventCount > 0">
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
        </template>

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
                defaultPageSize: 25,
                previousPageSize: ('pageSize' in this.$router.currentRoute.params) ? ( this.$router.currentRoute.params['pageSize'] !== undefined && this.$router.currentRoute.params['pageSize'] !== '' ? parseInt(this.$router.currentRoute.params['pageSize']) : this.defaultPageSize) : 25,
                selectedPageSize:  ('pageSize' in this.$router.currentRoute.params) ? ( this.$router.currentRoute.params['pageSize'] !== undefined && this.$router.currentRoute.params['pageSize'] !== '' ? parseInt(this.$router.currentRoute.params['pageSize']) : this.defaultPageSize) : 25,
                pageSizeValues: [5,10,25,50,100],
                isLoading: false,
                needRefresh: false,
                currentPage: ('pageNum' in this.$router.currentRoute.params) ? ( this.$router.currentRoute.params['pageNum'] !== undefined && this.$router.currentRoute.params['pageNum'] !== '' ? parseInt(this.$router.currentRoute.params['pageNum']) : 1) : 1,
                totalPageCount : 1,
                totalEventCount: 0,
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
                    this.loadEventSummaries().then(() => {
                        this.updatePagingRoute()
                    })

                }
            },
            pageRight() {
                // console.log('currentPage: ' + this.currentPage)
                // console.log(JSON.stringify(this.$store.state.log.eventSummaries[0]))
                if (this.currentPage < this.totalPageCount) {
                    this.currentPage++
                    this.previousPageSize = -1
                    this.loadEventSummaries().then(() => {
                        this.updatePagingRoute()
                    })

                }
            },
            gotoPage(pgNum) {
                if (this.currentPage !== pgNum && pgNum > 0 && pgNum <= this.totalPageCount) {
                    this.currentPage = pgNum
                    this.previousPageSize = -1
                    this.loadEventSummaries().then(() => {
                        this.updatePagingRoute()
                    })

                }
            },
            gotoLastPage() {
                if (this.currentPage != this.totalPageCount) {
                    this.currentPage = this.totalPageCount
                    this.previousPageSize = -1
                    this.loadEventSummaries().then(() => {
                        this.updatePagingRoute()
                    })

                }
            },
            async loadEventSummaries() {
                this.needRefresh = false
                this.isLoading = true
                let paramsObj = {testSession: this.sessionId, channel: this.channelName, itemsPerPage: this.selectedPageSize, page: this.currentPage, previousPageSize: this.previousPageSize}
                const that = this
                return this.$store.dispatch('loadEventSummaries', paramsObj)
                    .then(() => {
                        this.isLoading = false
                        this.totalEventCount = 0
                        this.totalPageCount = 0
                        if ('totalEventCount' in this.$store.state.log.eventSummaries[0]) {
                            const totalEventCount = this.$store.state.log.eventSummaries[0].totalEventCount
                            this.totalEventCount = totalEventCount
                        }
                        if ('totalPageableItems' in this.$store.state.log.eventSummaries[0]) {
                            const totalPageableItems = this.$store.state.log.eventSummaries[0].totalPageableItems
                            this.totalPageCount = totalPageableItems
                        }
                        if ('newPageNum' in this.$store.state.log.eventSummaries[0]) {
                            const newPageNum = this.$store.state.log.eventSummaries[0].newPageNum
                            this.currentPage = newPageNum
                        }
                    })
                    .catch(function (error) {
                        that.isLoading = false
                        that.totalEventCount = 0
                        that.totalPageCount = 0
                        that.currentPage = 0
                        console.error('loadEventSummaries Error: ' + error)
                    })

            },
            displayInstruction() {
               this.needRefresh = true
            },
            updatePagingRoute() {
                const currentRoutePath = this.$router.currentRoute.path
                const routePathToBe = `/session/${this.sessionId}/channel/${this.channelName}/logs/${this.selectedPageSize}/${this.currentPage}`
                // console.log('loglist current route: ' + currentRoutePath)
                // console.log('route to be: ' + routePathToBe)
                if (currentRoutePath !== routePathToBe) {
                    this.$router.replace(routePathToBe)
                }
            }
        },
        created() {
            this.loadEventSummaries().then(() => {
                this.updatePagingRoute()
            })

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
                    this.loadEventSummaries().then(() => {
                        this.updatePagingRoute()
                    })
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
                    this.loadEventSummaries().then(() => {
                        this.updatePagingRoute()
                    })

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
            'resourceType',  'sessionId', 'channelName', 'pageSize', 'pageNum'
        ],
        mixins: [eventMixin, errorHandlerMixin],
        name: "LogList"
    }
</script>

<style scoped>
    .loadingBkgText {
        color: black;
        font-size: smaller;
    }

    .pageNumNavMain {
        display: flex;
        flex-flow: row wrap;
        align-items: center;
        max-width: 850px;
        /*border: 1px solid black;*/
    }
    .pageItem {
        font-size: xx-small;
        width: 17px;
        align-self: center;
        text-align: right;
    }

    .pageNumLayoutMain {
        display: flex;
        horiz-align: center;
        justify-content: space-evenly;
        justify-self: center;
        margin-top: 6px;
    }
    .leftPad {
        width: 5%;
    }
    .centerContent {
        max-width: 90%;
        horiz-align: center;
        justify-content: space-evenly;
        justify-self: center;
        border: 1px solid steelblue;
    }
    .rightPad {
        width: 5%;
    }
    .currentPageNum {
        color: black;
        background-color: ivory;
        border: 2px inset black ;
    }

</style>
