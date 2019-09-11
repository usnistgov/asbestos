<template>
    <div>
        <router-view></router-view>
    </div>
</template>

<script>
    export default {
        name: "ChannelView",
        methods: {
            setChannelId() { // update state based on route
                if (this.channel !== this.channelId)
                    this.channel = this.channelId
            },
        },
        created() {
            if (this.$store.state.base.channelIds.length === 0)
                this.$store.dispatch('loadChannelNames')
            this.channel = this.channelId
        },
        computed: {
            channel: {
                set(name) {
                    if (name !== this.$store.state.base.channelId)
                        this.$store.commit('setChannelId', name)
                },
                get() {
                    return this.$store.state.base.channelId
                }
            }
        },
        watch: {
           'channelId': function(newVal) {
               if (this.channel !== newVal)
                   this.channel = newVal
           }
        },
        props: [ 'channelId']
    }
</script>

<style scoped>

</style>
