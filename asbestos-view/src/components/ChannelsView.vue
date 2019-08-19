<template>
    <div class="main">
        <div>
            Channels
        </div>
        <div class="nav">
<!--            <channel-nav></channel-nav>-->
            <div v-if="$route.params.index">
                <channel-view v-bind:index="$route.params.index"></channel-view>
            </div>
        </div>
        <router-view class="body" name="panel"></router-view>
    </div>
</template>

<script>
    import axios from 'axios'
    // import ChannelNav from "./ChannelNav"
    import ChannelView from "./ChannelView"

    export default {
        data() {
            return {

            }
        },
        components: {
            // ChannelNav,
            ChannelView
        },
        name: "ChannelsView",
        mounted() {
           // this.loadChannelNames()
        },
        methods: {
            loadChannelNames() {
                axios.get(`http://localhost:8081/proxy/channel`)
                    .then(response => {
                        this.$store.commit('installChannelIds', response.data)
                    })
                // .catch...
            }
        }
    }
</script>

<style scoped>

</style>
