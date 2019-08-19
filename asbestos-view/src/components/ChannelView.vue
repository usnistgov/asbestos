<template xmlns:v-slot="http://www.w3.org/1999/XSL/Transform">
    <div>
        <flash-message></flash-message>
        <div class="window">
            <div v-if="channel" class="grid-container">
                <div v-if="edit">
                    <img src="../assets/save.png" @click="save()"/>
                    <div class="divider"/>
                    <img src="../assets/cancel.png" @click="discard()"/>
                </div>
                <div v-else>
                    <img src="../assets/pencil-edit-button.png" @click="toggleEdit()"/>
                    <img src="../assets/add-button.png" @click="addChannel()"/>
                    <div class="divider"/>
                    <div class="divider"/>
                    <div class="divider"/>
                    <img src="../assets/delete-button.png" @click="deleteChannel()"/>
                </div>



                <label class="grid-name">Id</label>
                <div class="grid-item">{{ channel.channelId }}</div>

                <label class="grid-name">Test Session</label>
                <div class="grid-item">{{ channel.testSession }}</div>

                <label class="grid-name">Environment</label>
                <div v-if="edit" class="grid-item">
                    <select v-model="channel.environment">
                        <option v-for="e in $store.state.base.environments" :key="e">
                            {{e}}
                        </option>
                    </select>
                </div>
                <div v-else class="grid-item">{{ channel.environment }}</div>

                <label class="grid-name">Actor Type</label>
                <div class="grid-item">{{ channel.actorType }}</div>

                <label class="grid-name">Channel Type</label>
                <div v-if="edit" class="grid-item">
                    <select v-model="channel.channelType">
                        <option v-for="ct in $store.state.base.channelTypes" :key="ct">
                            {{ct}}
                        </option>
                    </select>
                </div>
                <div v-else class="grid-item">{{ channel.channelType }}</div>

                <label class="grid-name">Fhir Base</label>
                <div v-if="edit" class="grid-item">
                    <input v-model="channel.fhirBase">
                </div>
                <div v-else class="grid-item">{{ channel.fhirBase }}</div>

                <label class="grid-name">XDS Site Name</label>
                <div v-if="edit" class="grid-item">
                    <input v-model="channel.xdsSiteName">
                </div>
                <div v-else class="grid-item">{{ channel.xdsSiteName }}</div>

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
    const cloneDeep = require('clone-deep')
    import Vuetify from 'vuetify/lib'

    Vue.use(Vuetify)

    export default {
        data () {
            return {
                channel: null,
                edit: false
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
            addChannel() {

            },
            channelId() {
                  return this.channel.testSession + '__' + this.channel.channelId
            },
            deleteChannel() {
                const index = this.$route.params.channelIndex
                this.$store.commit('deleteChannel', this.channelId())
                if (index < this.$store.state.base.channelIds.length) {
                    this.$router.push('/session/' + this.$route.params.sessionId + '/channel/' + this.$route.params.channelIndex)
                } else {
                    this.$router.push('/session/' + this.$route.params.sessionId + '/channel/' + (this.$store.state.base.channelIds.length - 1))
                }
                this.fetch()
            },
            toggleEdit() {
                this.edit = !this.edit
            },
            save() {
                this.$store.commit('installChannel', cloneDeep(this.channel))
                this.fetch()
                this.toggleEdit()
            },
            discard() {
                this.fetch()
                this.toggleEdit()
            },
            fetch() {
                if (this.$store.state.base.channelIds.length === 0)
                    return
                const index = this.$route.params.channelIndex
                const channelId = this.$store.state.base.channelIds[index]
                if (this.$store.state.base.channels[index] === null) {
                    axios.get(`http://localhost:8081/proxy/channel/` + channelId)
                        .then(response => {
                            this.$store.commit('installChannel', response.data)
                            this.channel =  cloneDeep(this.$store.state.base.channels[index])
                        })
                    // .catch...
                } else {
                    this.channel = cloneDeep(this.$store.state.base.channels[index])
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
        /*font-weight: bold;*/
        /*background-color: rgba(255, 255, 255, 0.8);*/
        grid-column: 1;
        text-align: left;
    }
    .grid-item {
        /*background-color: rgba(255, 255, 255, 0.8);*/
        grid-column: 2;
        text-align: left;
    }
    .divider{
        width:5px;
        height:auto;
        display:inline-block;
    }
</style>
