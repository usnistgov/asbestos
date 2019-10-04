<template>
    <div>
        <div v-for="(eventId, eventi) in eventIds"
             :key="'Disp' + eventi">
            <div>
                <div  @click.self="selectEvent(eventId)" v-bind:class="[isEventPass(eventId) ? passClass : failClass, 'event-part']">
                    Event: {{ eventId }}
                </div>
                <div v-if="selected === eventId">
                    <router-view></router-view>
                </div>
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
            selectCurrent() {
                this.selectEvent(this.selected)
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
                return Object.keys(this.eventResult)
            },
            eventResult() {
                return this.$store.state.testRunner.clientTestResult[this.testId]
            },
        },
        watch: {
            //'eventResult': 'selectCurrent',
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
    .event-part {
        margin-left: 15px;
        margin-right: 15px;
        cursor: pointer;
        text-decoration: underline;
    }
</style>
