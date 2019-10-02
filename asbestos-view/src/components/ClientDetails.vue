<template>
    <div>
        <div v-for="(eventId, eventi) in eventIds" v-bind:class="[isEventPass(eventId) ? passClass : failClass, '']"
             :key="'Disp' + eventi">
            <div @click="selectEvent(eventId)">
                Event: {{ eventId }}
                <router-view></router-view>
<!--                <eval-details :session-id="sessionId"-->
<!--                              :channel-id="channelId"-->
<!--                              :test-collection="testCollection"-->
<!--                              :test-id="testId"-->
<!--                ></eval-details>-->
<!--                <eval-report-event-->
<!--                        :event-id="eventId"-->
<!--                        :test-report="eventResult[eventId]"-->
<!--                        :action-index="actionIndex"></eval-report-event>-->
            </div>
        </div>
    </div>
</template>

<script>

    export default {
        data() {
            return {
                passClass: 'pass',
                failClass: 'fail',
            }
        },
        methods: {
            selectEvent(name) {
                console.log(`selectEvent name = ${name}  selected = ${this.selected}`)
                if (this.selected === name)  { // unselect
                    this.$store.commit('setCurrentEvent', null)
                    const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${this.testId}`
                    this.$router.push(route)
                } else {
                    this.$store.commit('setCurrentEvent', name)
                    const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${this.testId}/event/${name}`
                    this.$router.push(route)
                }
            },

            isEventPass(eventId) {
                return this.eventResult[eventId].result === 'pass'
            },
        },
        computed: {
            selected() {
                return this.$store.state.testRunner.currentEvent
            },
            eventIds() {
                if (!this.eventResult) {
                    console.log('no event ids')
                    return null;
                }
                console.log(`eventIds: ${Object.getOwnPropertyNames(this.eventResult)}`)
                return Object.getOwnPropertyNames(this.eventResult)
            },
            eventResult() {
                console.log(`eventResult for evalId ${this.testId}`)
                console.log(`eventResult => ${this.$store.state.testRunner.clientTestResult[this.testId]}`)
                console.log(`evalIds are ${Object.getOwnPropertyNames(this.$store.state.testRunner.clientTestResult)}`)
                return this.$store.state.testRunner.clientTestResult[this.testId]
            },
        },
        watch: {
        },
        props: [
            'sessionId', 'channelId', 'testCollection', 'testId'
        ],
        components: {

        },
        name: "ClientDetails"
    }
</script>

<style scoped>

</style>
