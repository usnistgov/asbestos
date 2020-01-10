<template xmlns:v-slot="http://www.w3.org/1999/XSL/Transform">
    <div>
        <div class="window">
            <div v-if="channel">
                <div class="grid-container">
                    <div class="button-bar">
                        <div v-if="edit">
                            <div v-if="badNameMode">
                                Cannot save with this name - {{ badNameModeReason }}
                                <button class="cancel-button" @click="badNameCanceled">Continue</button>
                            </div>
                            <div v-else>
                                <div class="tooltip">
                                    <img id="save-button" src="../../assets/save.png" @click="save()"/>
                                    <span class="tooltiptext">Save</span>
                                </div>
                                <div class="divider"></div>
                                <div class="divider"></div>
                                <div class="divider"></div>
                                <div class="tooltip">
                                    <img id="cancel-edit-button" src="../../assets/cancel.png" @click="discard()"/>
                                    <span class="tooltiptext">Discard</span>
                                </div>
                            </div>
                        </div>
                        <div v-else>
                            <div v-if="ackMode">
                                Delete?
                                <button class="ok-button" @click="deleteAcked()">Ok</button>
                                <button class="cancel-button" @click="deleteCanceled">Cancel</button>
                            </div>
                            <div v-else>
                                <div class="tooltip">
                                    <img id="select-button" src="../../assets/select.png" @click="select()"/>
                                    <span class="tooltiptext">Select</span>
                                </div>
                                <div class="divider"></div>
                                <div class="tooltip">
                                    <img id="edit-button" src="../../assets/pencil-edit-button.png" @click="toggleEdit()"/>
                                    <span class="tooltiptext">Edit</span>
                                </div>
                                <div class="divider"></div>
                                <div class="tooltip">
                                    <img id="copy-button" src="../../assets/copy-document.png" @click="copy()"/>
                                    <span class="tooltiptext">Copy</span>
                                </div>
                                <div class="divider"></div>
                                <div class="divider"></div>
                                <div class="divider"></div>
                                <div class="tooltip">
                                    <img id="delete-button" src="../../assets/delete-button.png" @click="requestDelete()"/>
                                    <span class="tooltiptext">Delete</span>
                                </div>
                                <div class="divider"></div>
                                <div class="divider"></div>
                            </div>

                        </div>

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

                    <label class="grid-name">Channel Type</label>
                    <div v-if="edit" class="grid-item">
                        <select v-model="channel.channelType">
                            <option v-for="ct in $store.state.channel.channelTypes" :key="ct">
                                {{ct}}
                            </option>
                        </select>
                    </div>
                    <div v-else class="grid-item">{{ channel.channelType }}</div>

                    <label class="grid-name">Fhir Base</label>
                    <div v-if="edit" class="grid-item">
                        <input v-model="channel.fhirBase">
                        Only used with Channel Type fhir
                    </div>
                    <div v-else class="grid-item">{{ channel.fhirBase }}</div>

                    <label class="grid-name">XDS Site Name</label>
                    <div v-if="edit" class="grid-item">
                        <input v-model="channel.xdsSiteName">
                        Only used with Channel Type mhd
                    </div>
                    <div v-else class="grid-item">{{ channel.xdsSiteName }}</div>

                    <div v-if="!edit && !channel.fhirBase && !channel.xdsSiteName" class="channelError">
                        <div class="vdivider"></div>
                        <div class="vdivider"></div>
                        <div class="vdivider"></div>
                        <div class="vdivider"></div>
                        Warning: FhirBase or XDS Site Name must be present
                    </div>
                    <div v-if="!edit && channel.channelType === 'fhir' && !channel.fhirBase" class="channelError">
                        <div class="vdivider"></div>
                        <div class="vdivider"></div>
                        <div class="vdivider"></div>
                        <div class="vdivider"></div>
                        Warning: FHIR type is selected but no FHIR Base is configured
                    </div>
                    <div v-if="!edit && channel.channelType === 'mhd' && !channel.xdsSiteName" class="channelError">
                        <div class="vdivider"></div>
                        <div class="vdivider"></div>
                        <div class="vdivider"></div>
                        <div class="vdivider"></div>
                        Warning: MHD type is selected but no XDS Site Name is configured
                    </div>
                </div>
                <div v-if="!edit">
                    <p class="caption">Channel Base Address: </p>
                    <span>{{getChannelBase(channel)}}</span>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import Vue from 'vue'
    import {store} from "../../store"
    import {UtilFunctions, PROXY, CHANNEL} from '../../common/http-common'
    import VueFlashMessage from 'vue-flash-message';
    Vue.use(VueFlashMessage);
    require('vue-flash-message/dist/vue-flash-message.min.css')
    const cloneDeep = require('clone-deep')
    import { ButtonGroupPlugin, ButtonPlugin, ToastPlugin } from 'bootstrap-vue'
    Vue.use(ButtonGroupPlugin)
    Vue.use(ButtonPlugin)
    Vue.use(ToastPlugin)

    export default {
        data () {
            return {
                channel: null,  // channel object
                edit: false,
                isNew: false,
                originalChannelId: null,   // in case of delete
                discarding: false,  // for saving edits
                ackMode: false,  // for deleting
                badNameMode: false,
                badNameModeReason: null,
            }
        },
        props: [
            'sessionId', 'channelId'
        ],
        created() {
            this.fetch()
            this.showAck(true)
            // this.loadChannelBaseAddr()
        },
        watch: {  // when $route changes run fetch()
            '$route': 'fetch'
        },
        computed: {
            channelIds: {
                get() {
                    return this.$store.state.base.channelIds
                },
            },
        },
        methods: {
            msg(msg) {
                console.log(msg)
                this.$bvToast.toast(msg, {noCloseButton: true})
            },
            error(msg) {
                console.log(msg)
                this.$bvToast.toast(msg.message, {noCloseButton: true, title: 'Error'})
            },
            requestDelete() {
                this.ackMode = true
            },
            deleteAcked() {
                this.deleteChannel()
                this.ackMode = false
                const route = '/session/' + this.channel.testSession + '/channels'
                this.channel = undefined
                this.$router.push(route)
            },
            deleteCanceled() {
                this.ackMode = false
            },
            badNameCanceled() {
                this.badNameMode = false
            },
            getHidden() {
                return this.ackMode ? null : 'hidden'
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
                this.$router.push('/session/' + this.sessionId + '/channels/copy')
            },
            async deleteChannel() {
                try {
                    await PROXY.delete('channel/' + this.sessionId + '__' + this.channelId)
                    this.msg('Deleted')
                    this.$store.commit('deleteChannel', this.channelId)
                    await this.$store.dispatch('loadChannelNamesAndURLs')
                    this.$router.push('/session/' + this.sessionId + '/channels')
                } catch (error) {
                    this.error(error)
                }
            },
            toggleEdit() {
                this.edit = !this.edit
            },
            save() {
                const that = this
                if (this.isNew) {
                    if (this.isCurrentChannelIdNew()) {
                        this.badNameMode = true
                        this.badNameModeReason = `'new' is temporary and not acceptable`
                        return
                    }
                    if (this.isCurrentChannelIdBadPattern()) {
                        this.badNameMode = true
                        this.badNameModeReason = `Name may only contain a-z A-Z 0-9 _  and __ not allowed`
                        return
                    }
                    this.saveToServer(this.channel)
                    this.$store.commit('installChannel', cloneDeep(this.channel))
                    this.$store.commit('deleteChannel', this.originalChannelId) // original has been renamed
                    this.isNew = false
                    this.toggleEdit()
                    this.$router.push('/session/' + this.channel.testSession + '/channels/' + this.channel.channelId)
                    return
                }
                this.$store.commit('installChannel', cloneDeep(this.channel))
                CHANNEL.post('', this.channel)
                    .then(function () {
                        that.msg('Saved')
                    })
                    .catch(function (error) {
                        that.error(error)
                    })
                this.isNew = false
                this.toggleEdit()
                this.fetch()
            },
            async saveToServer(aChannel) {
                try {
                    await CHANNEL.post('', aChannel)
                    this.msg('New Channel Saved')
                    await this.$store.dispatch('loadChannelNamesAndURLs')
                } catch(error) {
                    this.error(error)
                }
            },
            discard() {
                if (this.isNew) {
                    this.deleteChannel()
                }
                this.isNew = false
                this.toggleEdit()
                this.discarding = true
                const route = '/session/' + this.channel.testSession + '/channels'
                this.channel = undefined
                this.$router.push(route)
            },
            isCurrentChannelIdNew() {
                return this.channel.channelId === 'new' || this.channel.channelId === 'copy'
            },
            isCurrentChannelIdBadPattern() {
                const id = this.channel.channelId
                const re = RegExp('^([a-zA-Z0-9_]+)$')
                const match = re.test(id)
                const re2 = RegExp('.*__.*')
                const match2 = re2.test(id)
                return !match || match2
            },
            isNewChannelId() {
                return this.channelId === 'new' || this.channelId === 'copy'
            },
            fetch() {
                if (this.channelId === undefined)
                    return
                if (this.channelIds.length === 0)
                    return
                this.originalChannelId = this.channelId
                if (this.isNewChannelId()) {
                    this.edit = true
                    this.isNew = true
                    this.channel = this.copyOfChannel()
                    this.discarding = false
                    return
                }
                const index = this.channelIndex(this.sessionId, this.channelId)
                if (index === -1) {
                    this.channel = null
                    return
                }
                    //const that = this
                    const fullId = `${this.sessionId}__${this.channelId}`

                    this.$store.dispatch('loadChannel', fullId)
                        .then(channel => {
                            this.channel = channel
                        })

                this.discarding = false
            },
            channelIndex(theSession, theChannelId) {
                return this.$store.state.base.channelIds.findIndex( function(channelId) {
                    return channelId === theChannelId
                })
            },
            getChannel() {
                return this.$store.state.base.channel
            },
            copyOfChannel() {
                const chan = this.getChannel()
                return cloneDeep(chan)
            },
            select() {
                if (this.channel.testSession === undefined || this.channel.channelId === undefined) {
                    return
                }
                const newRoute =  '/session/' + this.channel.testSession + '/channel/' + this.channel.channelId
                this.$store.commit('setChannelId', this.channel.channelId)
                this.$router.push(newRoute)
            },
            getChannelBase(channel) {
                return UtilFunctions.getChannelBase(channel)
            }
        },
        store: store,
        name: "ChannelEdit"
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
        grid-template-columns: auto;
        grid-template-rows: auto;
        grid-column-gap: 10px;
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
        font-size: 15px;
        padding: 0px 0px;
    }
    .cancel-button {
        font-size: 15px;
        padding: 0px 0px;
    }
    .button-bar {
        grid-column: 0 / span 2;
        alignment: left;
    }
    .tooltip {
        position: relative;
        display: inline-block;
        /*border-bottom: 1px dotted black;*/
    }

    .tooltip .tooltiptext {
        visibility: hidden;
        width: 120px;
        background-color: blue;
        color: #fff;

        bottom: 100%;
        left: 50%;
        margin-left: -60px;

        /* Position the tooltip */
        position: absolute;
        z-index: 1;
    }

    .tooltip:hover .tooltiptext {
        visibility: visible;
    }
    .channelError {
        color: red;
        text-align: left;
        /*border: 1px dotted black;*/
        grid-column: 1 / span 2;
    }
    .caption {
        font-weight: bold;
        font-size: larger;
    }
</style>
