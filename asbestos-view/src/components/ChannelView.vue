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
                    <div class="divider"/>
                    <div class="divider"/>
                    <div class="divider"/>
                    <img src="../assets/delete-button.png" @click="deleteChannel()"/>
                </div>



                <label class="grid-name">Id</label>
                <div v-if="isNew" class="grid-item">
                    <input v-model="channel.channelId">
                </div>
                <div v-else class="grid-item">{{ channel.channelId }}</div>

                <label class="grid-name">Test Session</label>
                <div v-if="isNew" class="grid-item">
                    <select v-model="channel.testSession">
                        <option v-for="e in $store.state.base.sessions" :key="e">
                            {{e}}
                        </option>
                    </select>
                </div>
                <div v-else class="grid-item">{{ channel.testSession }}</div>

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


    export default {
        data () {
            return {
                channel: null,
                edit: false,
                isNew: false,
                originalFullChannelId: null,   // in case of delete
                discarding: false
            }
        },
        props: [
            'sessionId', 'channelIndex'
        ],
        created() {
            this.fetch()
        },
        watch: {
            '$route': 'fetch'
        },
        methods: {
            fullChannelId() {
                  return this.channel.testSession + '__' + this.channel.channelId
            },
            deleteChannel() {
                const index = this.channelIndex
                this.$store.commit('deleteChannel', this.fullChannelId())
                if (index < this.fullChannelIds().length) {
                    this.$router.push('/session/' + this.sessionId + '/channel/' + this.channelIndex)
                    this.fetch()
                } else {
                    this.$router.push('/session/' + this.sessionId + '/channel/' + (this.fullChannelIds().length - 1))
                }
            },
            toggleEdit() {
                this.edit = !this.edit
            },
            save() {
                if (this.isNew)
                    this.$store.commit('deleteChannel', this.originalFullChannelId) // original has been renamed
                this.$store.commit('installChannel', cloneDeep(this.channel))
                this.isNew = false
                this.toggleEdit()
                this.fetch()
            },
            discard() {
                this.isNew = false
                this.toggleEdit()
                this.discarding = true
                this.fetch()
            },
            isNewChannelId(str) {
                return str.endsWith('__new')
            },
            fetch() {
                let index = this.channelIndex
                if (index === this.fullChannelIds().length) {
                    // happens as part of delete of new channel
                    this.channel = null
                    return
                }
                if (this.fullChannelIds().length === 0)
                    return
                const channelId = this.fullChannelIds()[index]
                this.originalFullChannelId = channelId
                if (this.isNewChannelId(channelId) && !this.discarding) {
                    this.edit = true
                    this.isNew = true
                    this.channel = cloneDeep(this.$store.state.base.channels[index])
                    this.discarding = false
                    return
                }
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
                this.discarding = false
            },
            fullChannelIds() {
                return this.$store.state.base.fullChannelIds
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
