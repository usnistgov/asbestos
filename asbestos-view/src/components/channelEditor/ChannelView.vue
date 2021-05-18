<template>
    <div>
        <router-view></router-view>
    </div>
</template>

<script>
    export default {
        name: "ChannelView",
        methods: {
            /*
            setChannelId() { // update state based on route
                if (this.channel !== this.channelName)
                    this.channel = this.channelName
            },

             */
        },
        created() {
            /*
            if (this.$store.state.base.channelIds.length === 0)
                this.$store.dispatch('loadChannelNames')
            this.channel = this.channelName

             */
        },
        computed: {
            /*
            channel: {
                set(name) {
                    if (name !== this.$store.state.base.channelName)
                        this.$store.commit('setChannelName', name)
                },
                get() {
                    return this.$store.state.base.channelName
                }
            }
             */
        },
        watch: {
            /*
           'channelName': function(newVal) {
               if (this.channel !== newVal)
                   this.channel = newVal
           }

             */
        },
        props: [ 'channelName']
    }
</script>

<style scoped>

</style>
