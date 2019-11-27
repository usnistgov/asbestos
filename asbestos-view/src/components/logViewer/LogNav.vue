<template>
    <div class="solid-boxed">
        <div class="nav-buttons">
            <div class="tooltip">
                <img id="return-button" class="selectable" src="../../assets/arrow-up.png" @click="up()"/>
                <span class="tooltiptext">Return</span>
            </div>
            <div v-if="moreToTheLeft" class="tooltip left-arrow-position">
                <img id="left-button" class="selectable" src="../../assets/left-arrow.png" @click="left()"/>
                <span class="tooltiptext">Previous</span>
            </div>
            <div v-if="moreToTheRight" class="tooltip right-arrow-position">
                <img id="right-button" class="selectable" src="../../assets/right-arrow.png" @click="right()"/>
                <span class="tooltiptext">Next</span>
            </div>
            <span class="item-count-position">
                Item {{ index + 1 }} of {{ this.$store.state.log.eventSummaries.length }}
            </span>
        </div>
    </div>
</template>

<script>

    export default {
        data() {
            return {

            }
        },
        methods: {
            up() {
                this.$router.back()
            },
            left() {
                if (this.moreToTheLeft) {
                    this.updateRoute(this.index - 1)
                }
            },
            right() {
                if (this.moreToTheRight) {
                    this.updateRoute(this.index + 1)
                }
            },
            updateRoute(newIndex) {
                if (!this.$store.state.log.eventSummaries)
                    return
                const summary = this.$store.state.log.eventSummaries[newIndex]
                if (summary)
                    this.$router.replace(`/session/${this.sessionId}/channel/${this.channelId}/lognav/${summary.eventName}`)
            },
        },
        created() {
            this.$store.commit('setCurrentEventIndex', this.index)
        },
        computed: {
            moreToTheLeft() {
                return this.index > 0
            },
            moreToTheRight() {
                return this.index + 1 < this.$store.state.log.eventSummaries.length
            }
        },
        props: [
            'index', 'sessionId', 'channelId'
        ],
        components: {

        },
        name: "LogNav"
    }
</script>

<style scoped>
    .nav-buttons {
        text-align: left;
    }
    .left-arrow-position {
        position: absolute;
        left: 90px;
    }
    .right-arrow-position {
        position: absolute;
        left: 130px;
    }
    .item-count-position {
        position: absolute;
        left: 200px;
    }
</style>
