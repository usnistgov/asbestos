<template>
    <div class="main">
        <div class="nav">
            <channel-nav></channel-nav>
            <channel-view></channel-view>
        </div>
        <router-view class="body" name="panel"></router-view>
    </div>
</template>

<script>
    import axios from 'axios'
    import ChannelNav from "./ChannelNav"
    import ChannelView from "./ChannelView"

    export default {
        data() {
            return {
                currentChannelId: null
            }
        },
        components: { ChannelNav, ChannelView },
        name: "ChannelsView",
        mounted() {
            this.loadChannelNames()
        },
        methods: {
            loadChannelNames() {
                axios.get(`http://localhost:8081/proxy/channel`)
                    .then(response => {
                        this.$store.commit('installChannelIds', response.data)
                        const channels = response.data
                        if (channels.length == 0) {
                            this.currentChannelId = null
                        } else {
                            this.currentChannelId = channels[0].channelId
                        }
                    })
                // .catch...
            }
        }
    }
</script>

<style scoped>

</style>
