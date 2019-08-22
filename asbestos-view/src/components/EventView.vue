<template>
    <div>

    </div>
</template>

<script>
    import {PROXY} from '../common/http-common'

    export default {
        data() {
            return {
                event: null
            }
        },
        props: [
            'eventId', 'channelId', 'sessionId'
        ],
        mounted() {
            const that = this
            PROXY.get(`${this.sessionId}/${this.channelId}/null/${this.eventId}`)
                .then(function (response) {
                    that.event = response.data
                })
                .catch(function (error) {
                    that.error(error)
                })
        },
        methods: {
            msg(msg) {
                console.log(msg)
                this.$bvToast.toast(msg, {noCloseButton: true})
            },
            error(err) {
                this.$bvToast.toast(err.message, {noCloseButton: true, title: 'Error'})
                console.log(err)
            },
        },
        name: "EventView"
    }
</script>

<style scoped>

</style>