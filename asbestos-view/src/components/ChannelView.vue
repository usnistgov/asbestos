<template xmlns:v-slot="http://www.w3.org/1999/XSL/Transform">
    <div>
        <flash-message></flash-message>
        <div class="window">
            <div v-if="channel">
                <div class="grid-container">
                    <div class="button-bar">
                        <div v-if="edit">
                            <img id="save-button" src="../assets/save.png" @click="save()"/>
                            <div class="divider"></div>
                            <div class="divider"></div>
                            <div class="divider"></div>
                            <img id="cancel-edit-button" src="../assets/cancel.png" @click="discard()"/>

                        </div>
                        <div v-else>
                            <img id="edit-button" src="../assets/pencil-edit-button.png" @click="toggleEdit()"/>
                            <div class="divider"></div>
                            <img id="copy-button" src="../assets/copy-document.png" @click="copy()"/>
                            <div class="divider"></div>
                            <div class="divider"></div>
                            <div class="divider"></div>
                            <img id="delete-button" src="../assets/delete-button.png" @click="deleteChannel()"/>
                            <div class="divider"></div>
                            <div class="divider"></div>

                            <button class="ok-button" v-bind:hidden="getHidden()">Ok</button>
                            <button class="cancel-button" v-bind:hidden="getHidden()">Cancel</button>
                        </div>

                        <b-tooltip target="save-button" title="Save"></b-tooltip>
                        <b-tooltip target="cancel-edit-button" title="Discard"></b-tooltip>

                        <b-tooltip target="edit-button" title="Edit"></b-tooltip>
                        <b-tooltip target="copy-button" title="Duplicate"></b-tooltip>
                        <b-tooltip target="delete-button" title="Delete"></b-tooltip>
                    </div>
                    <label class="grid-name">Id</label>
                    <div v-if="isNew" class="grid-item">
                        <input v-model="channel.channelId">
                    </div>
                    <div v-else class="grid-item">{{ channel.channelId }}</div>

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
    import { TooltipPlugin, ButtonGroupPlugin, ButtonPlugin } from 'bootstrap-vue'
    Vue.use(TooltipPlugin)
    Vue.use(ButtonGroupPlugin)
    Vue.use(ButtonPlugin)
    //    import { BButtonGroup } from 'bootstrap-vue'
    //Vue.component('b-button-group', BButtonGroup)

    export default {
        data () {
            return {
                channel: null,
                edit: false,
                isNew: false,
                originalFullChannelId: null,   // in case of delete
                discarding: false,
                ackHidden: ''
            }
        },
        props: [
            'sessionId', 'channelId'
        ],
        created() {
            this.fetch()
            this.showAck(true)
        },
        watch: {  // when $route changes run fetch()
            '$route': 'fetch'
        },
        methods: {
            getHidden() {
                return this.ackHidden
            },
            showAck(bool) {
                if (bool) {
                    this.ackHidden = null
                } else {
                    this.ackHidden = ''
                }
            },
            copy() {  // actually duplicate (a channel)
                let chan = cloneDeep(this.channel)
                chan.channelId = 'copy'
                this.$store.commit('installChannel', chan)
                this.$router.push('/session/' + this.sessionId + '/channel/copy')
            },
            fullChannelId() {
                return this.sessionId + '__' + this.channelId
            },
            deleteChannel() {
                this.$store.commit('deleteChannel', this.fullChannelId())
                this.$router.push('/session/' + this.sessionId + '/channel')
            },
            toggleEdit() {
                this.edit = !this.edit
            },
            save() {
                if (this.isNew) {
                    this.$store.commit('deleteChannel', this.originalFullChannelId) // original has been renamed
                    this.isNew = false
                    this.toggleEdit()
                    this.$store.commit('installChannel', cloneDeep(this.channel))
                    this.$router.push('/session/' + this.channel.testSession + '/channel/' + this.channel.channelId)
                    return
                }
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
            isNewChannelId() {
                return this.channelId === 'new' || this.channelId === 'copy'
            },
            fetch() {
                // let index = this.channelIndex
                // if (index === this.fullChannelIds().length) {
                //     // happens as part of delete of new channel
                //     this.channel = null
                //     return
                // }
                if (this.fullChannelIds().length === 0)
                    return
                //const channelId = this.fullChannelIds()[index]
                this.originalFullChannelId = this.fullChannelId()
                if (this.isNewChannelId() && !this.discarding) {
                    this.edit = true
                    this.isNew = true
                    this.channel = this.copyOfChannel()
                    this.discarding = false
                    return
                }
                const index = this.channelIndex(this.sessionId, this.channelId)
                if (this.$store.state.base.channels[index] === null) {
                    axios.get(`http://localhost:8081/proxy/channel/` + this.fullChannelId())
                        .then(response => {
                            this.$store.commit('installChannel', response.data)
                            this.channel =  this.copyOfChannel()
                        })
                    // .catch...
                } else {
                    this.channel = this.copyOfChannel()
                }
                this.discarding = false
            },
            fullChannelIds() {
                return this.$store.state.base.fullChannelIds
            },
            channelIndex(theSession, theChannelId) {
                const fullId = theSession + '__' + theChannelId
                return this.$store.state.base.fullChannelIds.findIndex( function(channelId) {
                    return channelId === fullId
                })
            },
            copyOfChannel() {
                const index = this.channelIndex(this.sessionId, this.channelId)
                if (index === -1) {
                    return null
                }
                return cloneDeep(this.$store.state.base.channels[index])
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
    .ok-button {
        font-size: 10px;
        padding: 0px 0px;
    }
    .cancel-button {
        font-size: 10px;
        padding: 0px 0px;
    }
    .button-bar {
        grid-column-start: 1;
        grid-column-end: 3;
    }
    .block {
    }
</style>
