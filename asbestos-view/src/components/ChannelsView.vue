<template>
    <div class="main">
        <div class="nav">
            <channel-panel></channel-panel>
        </div>
        <router-view class="body" name="panel"></router-view>
    </div>
</template>

<script>
    import axios from 'axios'
    import ChannelPanel from "./ChannelPanel"

    export default {
        data() {
            return {
            }
        },
        components: { ChannelPanel },
        name: "ChannelsView",
        mounted() {
            this.loadChannelNames()
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
