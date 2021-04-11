<template>
    <div class="container">
        <template v-if="channelName===undefined">
            <div class="nav channel-panel-header"></div>
            <div class="view">Please select a channel.</div>
        </template>
        <template v-else>
            <div class="nav channel-panel-header">
            </div>
            <div class="view" v-if="channel!==null || channel!==undefined">
                <h2>Channel Configuration</h2>
                <channel-edit :channel-name="channelName" :session-id="sessionId" :start-edit="startEditOpen"
                               class="view"></channel-edit> <!-- @onChLockStatus="updateChannelLockStatus" -->
                <!-- :the-channel="channel" -->
            </div>
        </template>
    </div>
</template>

<script>
    import {newChannel} from '@/types/channel'
    import ChannelEdit from "./ChannelEdit"
    import Vue from 'vue'
    import {TooltipPlugin, ToastPlugin} from 'bootstrap-vue'
    // import {ASBTS_USERPROPS, PROXY} from "@/common/http-common";
    // import SignIn from "../SignIn";

    Vue.use(TooltipPlugin)
    Vue.use(ToastPlugin)

    export default {
        data() {
            return {
                // lockAckMode: false,  // for deleting
                channelIds: null,
                channelId: null,
                // adding: false,
                // newChannelName: null,
                channel: null,
                // deleting: false,
                // editUserProps: ASBTS_USERPROPS,
                startEditOpen: false,
            }
        },
        props: [
            'sessionId', 'channelName'
        ],
        components: {
            ChannelEdit,
            // SignIn,
        },
        mounted() {
            /*
          this.$store.subscribe((mutation) => {
            switch(mutation.type) {
              case 'installChannel':
              case 'installChannelIds':
              case 'setSession':
              case 'loadChannelNames':
                this.channelIds = this.$store.getters.getChannelIdsForCurrentSession;
                break;
            }
          })
             */
        },
        created() {
            // this.updateChannelIds();
            // this.setChannelId(this.sessionId + '__' + this.channelName);
            // this.updateChannel();
        },
        watch: {
            /*
          channelId: function() {
              this.updateChannel();
          }
             */
        },
        computed: {
            /*
          currentChannelName() {
            if (!this.channelId) return null;
            if (this.channelId.split('__').length === 2)
              return this.channelId.split('__')[1];
            else
              return null;
          }
             */
        },
        methods: {
            /*
            updateChannelLockStatus(ch) {
                if (ch !== undefined && typeof (ch) === 'object' && ch['writeLocked'] !== undefined) {
                    this.channel.writeLocked = ch.writeLocked
                }
            },
            lockAcked() {
            },
            lockCanceled() {
                this.lockAckMode = ""
                this.lockAcked = null
                this.deleting = false
            },
            *
             */
            setChannelId(channelIdToSelect) {
                if (this.channelIds && this.channelIds.length > 0) {
                    let index = 0
                    if (channelIdToSelect !== undefined && typeof (channelIdToSelect) === 'string' && channelIdToSelect.length > 0) {
                        if (this.channelIds.includes(channelIdToSelect)) {
                            index = this.channelIds.indexOf(channelIdToSelect)
                        }
                    }
                    this.channelId = this.channelIds[index];
                } else
                    this.channelId = null;
            },
            /*
            async confirmDel() {
                try {
                    if (!this.channel.writeLocked) {
                        await PROXY.delete('channel/' + this.sessionId + '__' + this.currentChannelName)
                    } else if (this.editUserProps.bapw !== "") {
                        await PROXY.delete('channelGuard/' + this.channelId, {
                            auth: {
                                username: this.editUserProps.bauser,
                                password: this.editUserProps.bapw
                            }
                        })
                    }
                    this.msg('Deleted')
                    this.localDelete(this.channelId);
                    this.$store.commit('deleteChannel', this.channelId)
                    await this.$store.dispatch('loadChannelIds')
                    this.setChannelId();
                    //this.$router.push('/session/' + this.sessionId + '/channels')
                } catch (error) {
                    this.error(error);
                }
                this.adding = false;
                this.cancelDel()
            },
            localDelete(theChannelId) {
                const index = this.channelIds.findIndex(function (channelId) {
                    return channelId === theChannelId;
                })
                if (index === -1)
                    return;
                this.channelIds.splice(index, 1);
                this.channel = null;
            },

            cancelDel() {
                this.deleting = false;
                this.lockAcked = null;
                this.lockAckMode = "";
            },

            del() {
                this.deleting = true;
            },

            guardedFn(str, fn) {
                if ('Delete' === str) {
                    this.adding = false;
                }
                if (typeof fn === 'function') {
                    if (this.channel.writeLocked) {
                        if (this.editUserProps.signedIn) {
                            this.lockAcked = null
                            fn.call()
                        } else {
                            const that = this
                            this.lockAcked = function () {
                                that.guardedFn(str, fn)
                            }
                            this.lockAckMode = str + ": "
                        }
                    } else {
                        fn.call()
                    }
                }
            },

             */

            updateChannel() {
                if (!this.channel || this.channel.channelName !== this.currentChannelName) {
                    if (this.channelId !== null) {
                        this.$store.dispatch('loadChannel', this.channelId)
                            .then(channel => {
                                this.channel = channel
                            })
                    }
                }
            },

            addBtnClick() {
                this.newChannelName = null
                this.cancelDel()
                this.adding = true
            },
            doAdd() {
                this.channel = newChannel();
                this.channel.channelName = this.newChannelName;
                if (this.isCurrentChannelIdBadPattern()) {
                    this.$store.commit('setError', `Name may only contain a-z A-Z 0-9 and -.`);
                    return;
                }
                this.channel.testSession = this.sessionId;
                //this.$store.commit('setChannel', this.channel)
                this.adding = false;
                this.channelId = `${this.sessionId}__${this.newChannelName}`;
                if (this.channelIds === null)
                    this.channelIds = [];
                //this.channelIds.push(this.channelId);
                this.$store.commit('installChannel', this.channel);
                this.$store.commit('setChannelIsNew', true);
                this.pushChannelRoute();
            },
            isCurrentChannelIdBadPattern() {
                const name = this.channel.channelName
                const re = RegExp('^([a-zA-Z0-9-]+)$')
                const match = re.test(name)
                const re2 = RegExp('.*__.*')
                const match2 = re2.test(name)
                return !match || match2
            },
            cancelAdd() {
                this.newChannelName = null;
                this.adding = false;
            },
            updateChannelIds() {
                this.channelIds = this.$store.getters.getChannelIdsForCurrentSession;
            },
            // for create a new channel
            pushNewChannelRoute() {
                return this.$router.push(this.newChannelRoute())
            },
            pushChannelRoute() {
                const route = this.channelRoute();
                if (route)
                    return this.$router.push(route);
            },
            channelRoute() {
                if (!this.channel)
                    return null;
                return '/session/' + this.channel.testSession + '/channels/' + this.channel.channelName;
            },
            /*
            newChannelRoute() {
                let chan = newChannel()
                chan.testSession = this.sessionId
                chan.channelName = 'new'
                chan.channelType = 'fhir'
                this.$store.commit('setChannel', chan)
                this.$store.commit('setChannelIsNew');
                return '/session/' + this.sessionId + '/channels/new'
            },
            *
             */
            channelsLink(channelId) {
                const chan = channelId.split('__', 2);
                const session = chan[0];
                const channelName = chan[1];
                return '/session/' + session + '/channels/' + channelName;
            },
            msg(msg) {
                console.log(msg)
                this.$bvToast.toast(msg, {noCloseButton: true})
            },
            error(err) {
                this.$bvToast.toast(err.message, {noCloseButton: true, title: 'Error'})
                console.log(err)
            },
        },
        name: "ChannelNav"
    }
</script>

<style scoped>
    .container {
        display: grid;
        grid-template-columns: 1fr 2fr;
        grid-template-areas: 'nav view';
        align-content: start;
    }

    .nav {
        grid-area: nav;
        /*border: 1px dotted black;*/
        text-align: left;
        /*width: 100%;*/
    }

    .view {
        grid-area: view;
        text-align: left;
        /*width: 100%;*/
        /*border: 1px dotted black;*/
    }

    .channel-panel-header {
        font-weight: bold;
    }

    .element-nav {
        position: relative;
        left: 0px;
    }
</style>
