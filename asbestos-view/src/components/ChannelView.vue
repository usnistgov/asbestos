<template>
    <div>
        <flash-message></flash-message>
        <div>
            Channel {{ $route.params.channelIndex }}
        </div>
        <div class="window">
            <div v-if="channel" class="grid-container">
                <label class="grid-name">Id</label>
                <div class="grid-item">{{ channel.channelId }}</div>

                <label class="grid-name">Test Session</label>
                <div class="grid-item">{{ channel.testSession }}</div>

                <label class="grid-name">Environment</label>
                <div class="grid-item">{{ channel.environment }}</div>

                <label class="grid-name">Actor Type</label>
                <div class="grid-item">{{ channel.actorType }}</div>

                <label class="grid-name">Channel Type</label>
                <div class="grid-item">{{ channel.channelType }}</div>

                <label class="grid-name">Fhir Base</label>
                <div class="grid-item">{{ channel.fhirBase }}</div>

                <label class="grid-name">XDS Site Name</label>
                <div class="grid-item">{{ channel.xdsSiteName }}</div>

            </div>
        </div>
    </div>
</template>

<script>
    import Vue from 'vue'
    import {store} from "../store"
    import axios from 'axios'
    import VueFlashMessage from 'vue-flash-message';
    Vue.use(VueFlashMessage);
    require('vue-flash-message/dist/vue-flash-message.min.css')

    export default {
        data () {
            return {
                channel: null
            }
        },
        props: [

        ],
        created() {
            this.fetch()
        },
        watch: {
            '$route': 'fetch'
        },
        methods: {
            fetch() {
                if (this.$store.state.base.channelIds.length === 0)
                    return
                const index = this.$route.params.channelIndex
                const channelId = this.$store.state.base.channelIds[index]
                if (this.$store.state.base.channels[index] === null) {
                    axios.get(`http://localhost:8081/proxy/channel/` + channelId)
                        .then(response => {
                            this.$store.commit('installChannel', response.data)
                            this.channel =  this.$store.state.base.channels[index]
                        })
                    // .catch...
                } else {
                    this.channel =  this.$store.state.base.channels[index]
                }
            }
        },
        store: store,
        name: "ChannelView"
    }
</script>

<style scoped>
    .window {
        display: grid;
        grid-template-columns: auto auto;
        margin: 5px;
    }
    .grid-container {
        display: grid;
        grid-template-columns: 15ch auto;
        grid-template-rows: auto;

    }
    .grid-name {
        font-weight: bold;
        background-color: rgba(255, 255, 255, 0.8);
        grid-column: 1;
    }
    .grid-item {
        background-color: rgba(255, 255, 255, 0.8);
        grid-column: 2;
    }
</style>
